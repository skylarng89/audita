package io.audita.infrastructure.service;

import io.audita.application.port.AuditTrailPort;
import io.audita.application.port.AuditTrailPort.IntegrityResult;
import io.audita.domain.model.TenantStatus;
import io.audita.infrastructure.persistence.entity.TenantEntity;
import io.audita.infrastructure.persistence.repository.TenantRepository;
import io.audita.infrastructure.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Service
public class AuditIntegrityScheduler {

    private static final Logger log = LoggerFactory.getLogger(AuditIntegrityScheduler.class);

    private final TenantRepository tenantRepository;
    private final AuditTrailPort auditTrailPort;
    private final TransactionTemplate transactionTemplate;

    public AuditIntegrityScheduler(
            TenantRepository tenantRepository,
            AuditTrailPort auditTrailPort,
            PlatformTransactionManager transactionManager) {
        this.tenantRepository = tenantRepository;
        this.auditTrailPort = auditTrailPort;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.transactionTemplate.setReadOnly(true);
    }

    @Scheduled(fixedDelayString = "${audita.audit.integrity-check-delay-ms:3600000}")
    public void verifyIntegrity() {
        List<TenantEntity> activeTenants = tenantRepository.findByStatus(TenantStatus.ACTIVE);

        for (TenantEntity tenant : activeTenants) {
            String tenantSlug = tenant.getSlug();
            try {
                TenantContext.setCurrentTenant(tenantSlug);
                IntegrityResult result = transactionTemplate.execute(
                        _ -> auditTrailPort.verifyIntegrity());
                if (result != null && result.tampered()) {
                    log.warn("Audit log tampering detected: tenant={} tamperedRecords={} totalRecords={}",
                            tenantSlug, result.tamperedRecordIds().size(), result.totalRecords());
                }
            } catch (Exception e) {
                log.error("Audit integrity check failed: tenant={}", tenantSlug, e);
            } finally {
                TenantContext.clear();
            }
        }
    }
}
