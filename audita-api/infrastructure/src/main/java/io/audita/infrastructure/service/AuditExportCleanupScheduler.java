package io.audita.infrastructure.service;

import io.audita.domain.model.TenantStatus;
import io.audita.infrastructure.persistence.entity.TenantEntity;
import io.audita.infrastructure.persistence.repository.TenantRepository;
import io.audita.infrastructure.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AuditExportCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuditExportCleanupScheduler.class);

    private final TenantRepository tenantRepository;
    private final AuditExportService auditExportService;
    private final TransactionTemplate transactionTemplate;

    @Value("${audita.audit.export.cleanup-retention-hours:168}")
    private int cleanupRetentionHours;

    public AuditExportCleanupScheduler(
            TenantRepository tenantRepository,
            AuditExportService auditExportService,
            PlatformTransactionManager transactionManager) {
        this.tenantRepository = tenantRepository;
        this.auditExportService = auditExportService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Scheduled(fixedDelayString = "${audita.audit.export.cleanup-delay-ms:3600000}")
    public void cleanup() {
        List<TenantEntity> activeTenants = tenantRepository.findByStatus(TenantStatus.ACTIVE);
        OffsetDateTime now = OffsetDateTime.now();

        for (TenantEntity tenant : activeTenants) {
            String tenantSlug = tenant.getSlug();
            try {
                TenantContext.setCurrentTenant(tenantSlug);
                transactionTemplate.executeWithoutResult(status ->
                        auditExportService.cleanupForCurrentTenant(now, cleanupRetentionHours));
            } catch (Exception error) {
                log.error("Audit export cleanup failed for tenant={}", tenantSlug, error);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
