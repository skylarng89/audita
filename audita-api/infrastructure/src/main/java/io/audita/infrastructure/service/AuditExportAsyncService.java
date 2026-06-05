package io.audita.infrastructure.service;

import io.audita.application.port.AuditTrailPort;
import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.infrastructure.persistence.entity.AuditExportRequestEntity;
import io.audita.infrastructure.persistence.repository.AuditExportRequestRepository;
import io.audita.infrastructure.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

@Service
public class AuditExportAsyncService {

    private static final Logger log = LoggerFactory.getLogger(AuditExportAsyncService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final AuditTrailPort auditTrailPort;
    private final AuditExportRequestRepository auditExportRequestRepository;
    private final EmailService emailService;

    @Value("${audita.app.base-url:http://localhost:3000}")
    private String appBaseUrl;

    @Value("${audita.audit.export.storage-path:/tmp/audita-exports}")
    private String exportStoragePath;

    public AuditExportAsyncService(
            AuditTrailPort auditTrailPort,
            AuditExportRequestRepository auditExportRequestRepository,
            EmailService emailService) {
        this.auditTrailPort = auditTrailPort;
        this.auditExportRequestRepository = auditExportRequestRepository;
        this.emailService = emailService;
    }

    @Async
    @Transactional
    public void generateAsync(UUID exportRequestId, String tenantSlug, int expiryHours) {
        TenantContext.setCurrentTenant(tenantSlug);
        try {
            AuditExportRequestEntity request = auditExportRequestRepository.findById(exportRequestId)
                    .orElseThrow(() -> new DomainNotPermittedException("NOT_FOUND", "Export request not found."));

            List<AuditTrailPort.AuditLogEntry> entries = auditTrailPort.export(
                    request.getActorEmailFilter(),
                    request.getActionTypeFilter(),
                    request.getEntityTypeFilter(),
                    request.getDateFrom(),
                    request.getDateTo());

            Path directory = Path.of(exportStoragePath, tenantSlug).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            String fileName = "audit-trail-" + exportRequestId + ".csv.gz";
            Path filePath = directory.resolve(fileName).normalize();
            if (!filePath.startsWith(directory)) {
                throw new IOException("Invalid export path");
            }

            writeCsvGzip(filePath, entries);

            String token = generateToken();
            OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(expiryHours);

            request.setFileName(fileName);
            request.setFileStoragePath(filePath.toString());
            request.setDownloadToken(token);
            request.setTokenExpiresAt(expiresAt);
            request.setCompletedAt(OffsetDateTime.now());
            request.setStatus(AuditExportRequestEntity.Status.READY);
            request.setFailureReason(null);
            auditExportRequestRepository.save(request);

            String downloadUrl = appBaseUrl + "/api/v1/audit-trail/exports/download/" + token;
            emailService.sendAuditExportReadyEmail(
                    request.getRequestedByEmail(),
                    downloadUrl,
                    expiresAt,
                    request.getDateFrom(),
                    request.getDateTo());
        } catch (Exception error) {
            log.error("Failed generating audit export requestId={} tenant={}", exportRequestId, tenantSlug, error);
            auditExportRequestRepository.findById(exportRequestId).ifPresent(request -> {
                request.setStatus(AuditExportRequestEntity.Status.FAILED);
                request.setFailureReason(error.getMessage());
                request.setCompletedAt(OffsetDateTime.now());
                auditExportRequestRepository.save(request);
            });
        } finally {
            TenantContext.clear();
        }
    }

    private void writeCsvGzip(Path filePath, List<AuditTrailPort.AuditLogEntry> entries) throws IOException {
        try (GZIPOutputStream gzipOutputStream = new GZIPOutputStream(Files.newOutputStream(filePath));
                Writer writer = new OutputStreamWriter(gzipOutputStream, StandardCharsets.UTF_8)) {
            writer.write("id,actorFullName,actorEmail,actionType,entityType,entityId,ipAddress,createdAt\n");
            for (AuditTrailPort.AuditLogEntry entry : entries) {
                writer.write(String.join(",",
                        csv(entry.id() == null ? "" : entry.id().toString()),
                        csv(entry.actorFullName()),
                        csv(entry.actorEmail()),
                        csv(entry.actionType()),
                        csv(entry.entityType()),
                        csv(entry.entityId() == null ? "" : entry.entityId().toString()),
                        csv(entry.ipAddress()),
                        csv(entry.createdAt() == null ? "" : entry.createdAt().toString())));
                writer.write("\n");
            }
        }
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
