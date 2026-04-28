package io.audita.infrastructure.service;

import io.audita.infrastructure.persistence.entity.ActivityStreamEntity;
import io.audita.infrastructure.persistence.entity.ChangeRequestEntity;
import io.audita.infrastructure.persistence.entity.CrApproverEntity;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.ActivityStreamRepository;
import io.audita.infrastructure.persistence.repository.ChangeRequestRepository;
import io.audita.infrastructure.persistence.repository.CrApproverRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional
public class SlaMonitoringService {

    private final ChangeRequestRepository changeRequestRepository;
    private final CrApproverRepository crApproverRepository;
    private final ActivityStreamRepository activityStreamRepository;
    private final NotificationService notificationService;
    private final EmailService emailService;

    public SlaMonitoringService(ChangeRequestRepository changeRequestRepository,
                                CrApproverRepository crApproverRepository,
                                ActivityStreamRepository activityStreamRepository,
                                NotificationService notificationService,
                                EmailService emailService) {
        this.changeRequestRepository = changeRequestRepository;
        this.crApproverRepository = crApproverRepository;
        this.activityStreamRepository = activityStreamRepository;
        this.notificationService = notificationService;
        this.emailService = emailService;
    }

    @Scheduled(fixedDelayString = "${audita.sla.monitor-delay-ms:60000}")
    public void evaluate() {
        OffsetDateTime now = OffsetDateTime.now();
        processWarnings(now);
        processBreaches(now);
    }

    private void processWarnings(OffsetDateTime now) {
        List<ChangeRequestEntity> warningCandidates = changeRequestRepository.findSlaWarning(now, now.plusHours(1));
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
}
