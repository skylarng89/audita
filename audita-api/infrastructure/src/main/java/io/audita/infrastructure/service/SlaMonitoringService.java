package io.audita.infrastructure.service;

import io.audita.domain.model.TenantStatus;
import io.audita.infrastructure.persistence.entity.ActivityStreamEntity;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.CrApproverEntity;
import io.audita.infrastructure.persistence.entity.TenantEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.ActivityStreamRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import io.audita.infrastructure.persistence.repository.CrApproverRepository;
import io.audita.infrastructure.persistence.repository.OrgSettingRepository;
import io.audita.infrastructure.persistence.repository.TenantRepository;
import io.audita.infrastructure.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SlaMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(SlaMonitoringService.class);

    private final ChangeRequestRepository changeRequestRepository;
    private final CrApproverRepository crApproverRepository;
    private final ActivityStreamRepository activityStreamRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final TenantRepository tenantRepository;
    private final OrgSettingRepository orgSettingRepository;
    private final TransactionTemplate transactionTemplate;

    public SlaMonitoringService(ChangeRequestRepository changeRequestRepository,
                                CrApproverRepository crApproverRepository,
                                ActivityStreamRepository activityStreamRepository,
                                NotificationService notificationService,
                                EmailService emailService,
                                TenantRepository tenantRepository,
                                OrgSettingRepository orgSettingRepository,
                                PlatformTransactionManager transactionManager) {
        this.changeRequestRepository = changeRequestRepository;
        this.crApproverRepository = crApproverRepository;
        this.activityStreamRepository = activityStreamRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
        this.tenantRepository = tenantRepository;
        this.orgSettingRepository = orgSettingRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(fixedDelayString = "${audita.sla.monitor-delay-ms:60000}")
    public void evaluate() {
        List<TenantEntity> activeTenants = tenantRepository.findByStatus(TenantStatus.ACTIVE);
        OffsetDateTime now = OffsetDateTime.now();

        for (TenantEntity tenant : activeTenants) {
            String tenantSlug = tenant.getSlug();
            try {
                TenantContext.setCurrentTenant(tenantSlug);
                transactionTemplate.executeWithoutResult(status -> {
                    processWarnings(now);
                    processBreaches(now);
                });
            } catch (Exception ex) {
                log.error("SLA monitoring failed for tenant {}", tenantSlug, ex);
            } finally {
                TenantContext.clear();
            }
        }
    }

    private void processWarnings(OffsetDateTime now) {
        int warningHours = resolveWarningBeforeHours();
        List<ChangeRequestEntity> warningCandidates = changeRequestRepository.findSlaWarning(now, now.plusHours(warningHours));
        for (ChangeRequestEntity cr : warningCandidates) {
            activityStreamRepository.save(new ActivityStreamEntity(
                    cr,
                    null,
                    "CR_SLA_WARNING",
                    Map.of("slaDeadline", cr.getSlaDeadline().toString())
            ));
            notifyParticipants(cr, "SLA_WARNING", "SLA warning for " + cr.getTitle());
        }
    }

    private void processBreaches(OffsetDateTime now) {
        List<ChangeRequestEntity> breached = changeRequestRepository.findSlaBreached(now);
        for (ChangeRequestEntity cr : breached) {
            cr.markSlaBreached();
            changeRequestRepository.save(cr);

            activityStreamRepository.save(new ActivityStreamEntity(
                    cr,
                    null,
                    "CR_SLA_BREACHED",
                    Map.of("slaDeadline", cr.getSlaDeadline().toString(), "breachedAt", now.toString())
            ));

            notifyParticipants(cr, "SLA_BREACH", "SLA breached for " + cr.getTitle());
        }
    }

    private void notifyParticipants(ChangeRequestEntity cr, String type, String title) {
        Set<UserEntity> recipients = new HashSet<>();
        if (cr.getCreatedBy() != null) {
            recipients.add(cr.getCreatedBy());
        }
        List<CrApproverEntity> approvers = crApproverRepository.findByChangeRequestIdOrderByPositionAsc(cr.getId());
        for (CrApproverEntity approver : approvers) {
            recipients.add(approver.getUser());
        }

        for (UserEntity recipient : recipients) {
            notificationService.createAndPush(
                    recipient.getId(),
                    type,
                    title,
                    "Change request: " + cr.getTitle(),
                    "/change-requests/" + cr.getId()
            );
            if ("SLA_BREACH".equals(type)) {
                emailService.sendSlaBreachEmail(
                        recipient.getEmail(),
                        recipient.getFullName(),
                        cr.getTitle(),
                        cr.getId().toString()
                );
            }
        }
    }

    private int resolveWarningBeforeHours() {
        return orgSettingRepository.findById("sla.warning_before_hours")
                .map(setting -> setting.getValue().trim())
                .map(value -> {
                    try {
                        int parsed = Integer.parseInt(value);
                        return parsed > 0 ? parsed : 1;
                    } catch (NumberFormatException ex) {
                        return 1;
                    }
                })
                .orElse(1);
    }
}
