package io.audita.infrastructure.service;

import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.infrastructure.persistence.entity.AuditExportRequestEntity;
import io.audita.infrastructure.persistence.repository.OrgSettingRepository;
import io.audita.infrastructure.persistence.repository.AuditExportRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AuditExportService {

    private static final Logger log = LoggerFactory.getLogger(AuditExportService.class);

    private final AuditExportRequestRepository auditExportRequestRepository;
    private final OrgSettingRepository orgSettingRepository;
    private final AuditExportAsyncService auditExportAsyncService;

    private static final String AUDIT_EXPORT_LINK_EXPIRY_HOURS_KEY = "audit.export_link_expiry_hours";

    @Value("${audita.audit.export.link-expiry-hours:24}")
    private int defaultLinkExpiryHours;

    public AuditExportService(
            AuditExportRequestRepository auditExportRequestRepository,
            OrgSettingRepository orgSettingRepository,
            AuditExportAsyncService auditExportAsyncService) {
        this.auditExportRequestRepository = auditExportRequestRepository;
        this.orgSettingRepository = orgSettingRepository;
        this.auditExportAsyncService = auditExportAsyncService;
    }

    public UUID queueExport(
            UUID actorId,
            String actorEmail,
            String tenantSlug,
            String actorEmailFilter,
            String actionType,
            String entityType,
            LocalDate from,
            LocalDate to,
            Integer linkExpiryHoursOverride) {
        AuditExportRequestEntity request = new AuditExportRequestEntity();
        request.setRequestedByUserId(actorId);
        request.setRequestedByEmail(actorEmail);
        request.setActorEmailFilter(blankToNull(actorEmailFilter));
        request.setActionTypeFilter(blankToNull(actionType));
        request.setEntityTypeFilter(blankToNull(entityType));
        request.setDateFrom(from);
        request.setDateTo(to);
        request.setStatus(AuditExportRequestEntity.Status.PENDING);
        AuditExportRequestEntity saved = auditExportRequestRepository.save(request);
        int expiryHours = resolveLinkExpiryHours(linkExpiryHoursOverride);
        auditExportAsyncService.generateAsync(saved.getId(), tenantSlug, expiryHours);
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public AuditExportRequestEntity getByTokenOrThrow(String token) {
        AuditExportRequestEntity request = auditExportRequestRepository.findByDownloadToken(token)
                .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Export download not found."));
        if (request.getStatus() == AuditExportRequestEntity.Status.EXPIRED ||
                request.getTokenExpiresAt() == null ||
                request.getTokenExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new DomainNotPermittedException("EXPIRED", "Export link has expired.");
        }
        if (request.getStatus() != AuditExportRequestEntity.Status.READY) {
            throw new DomainNotPermittedException("NOT_READY", "Export file is not ready.");
        }
        return request;
    }

    public void cleanupForCurrentTenant(OffsetDateTime now, int retentionHours) {
        expireTokens(now);
        deleteStaleExports(now.minusHours(Math.max(retentionHours, 1)));
    }

    private void expireTokens(OffsetDateTime now) {
        List<AuditExportRequestEntity> expired = auditExportRequestRepository
                .findByStatusAndTokenExpiresAtBefore(AuditExportRequestEntity.Status.READY, now);
        for (AuditExportRequestEntity request : expired) {
            request.setStatus(AuditExportRequestEntity.Status.EXPIRED);
            request.setDownloadToken(null);
            auditExportRequestRepository.save(request);
        }
    }

    private void deleteStaleExports(OffsetDateTime cutoff) {
        List<AuditExportRequestEntity> stale = auditExportRequestRepository
                .findByStatusInAndCompletedAtBefore(
                        List.of(AuditExportRequestEntity.Status.EXPIRED, AuditExportRequestEntity.Status.FAILED),
                        cutoff);
        for (AuditExportRequestEntity request : stale) {
            deleteExportFile(request.getFileStoragePath());
            auditExportRequestRepository.delete(request);
        }
    }

    private void deleteExportFile(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(Path.of(storagePath).toAbsolutePath().normalize());
        } catch (IOException error) {
            log.warn("Failed deleting stale audit export file path={}", storagePath, error);
        }
    }

    private int resolveLinkExpiryHours(Integer override) {
        int configuredDefault;
        try {
            configuredDefault = orgSettingRepository.findById(AUDIT_EXPORT_LINK_EXPIRY_HOURS_KEY)
                    .map(setting -> setting.getValue() == null ? "" : setting.getValue().trim())
                    .filter(value -> !value.isEmpty())
                    .map(Integer::parseInt)
                    .orElse(defaultLinkExpiryHours);
        } catch (NumberFormatException _) {
            configuredDefault = defaultLinkExpiryHours;
        }
        if (override == null) {
            return configuredDefault;
        }
        if (override < 1) {
            return 1;
        }
        if (override > 168) {
            return 168;
        }
        return override;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
