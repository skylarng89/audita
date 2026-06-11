package io.audita.api.controller;

import io.audita.api.dto.response.AuditLogResponse;
import io.audita.api.dto.response.PageResponse;
import io.audita.api.security.UserPrincipal;
import io.audita.application.port.AuditTrailPort;
import io.audita.infrastructure.persistence.entity.AuditExportRequestEntity;
import io.audita.infrastructure.service.AuditExportService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/audit-trail")
@PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR')")
public class AuditTrailController {

    private static final DateTimeFormatter CSV_TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final AuditTrailPort auditTrailPort;
    private final AuditExportService auditExportService;

    public AuditTrailController(AuditTrailPort auditTrailPort, AuditExportService auditExportService) {
        this.auditTrailPort = auditTrailPort;
        this.auditExportService = auditExportService;
    }

    @GetMapping
    public PageResponse<AuditLogResponse> query(
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {

        LocalDate effectiveTo = to == null ? LocalDate.now() : to;
        LocalDate effectiveFrom = from == null ? effectiveTo.minusDays(30) : from;

        return PageResponse.from(
                auditTrailPort.query(actorEmail, actionType, entityType, effectiveFrom, effectiveTo, pageable),
                AuditLogResponse::from);
    }

    @GetMapping(value = "/verify")
    public ResponseEntity<Map<String, Object>> verifyIntegrity() {
        AuditTrailPort.IntegrityResult result = auditTrailPort.verifyIntegrity();
        Map<String, Object> response = Map.of(
                "tampered", result.tampered(),
                "totalRecords", result.totalRecords(),
                "verifiableRecords", result.verifiableRecords(),
                "tamperedRecordIds", result.tamperedRecordIds());
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to)
            throws IOException {

        LocalDate effectiveTo = to == null ? LocalDate.now() : to;
        LocalDate effectiveFrom = from == null ? effectiveTo.minusDays(30) : from;

        List<AuditTrailPort.AuditLogEntry> entries = auditTrailPort.export(actorEmail, actionType, entityType,
                effectiveFrom, effectiveTo);

        String csv;
        try (StringWriter sw = new StringWriter()) {
            sw.write("id,actorFullName,actorEmail,actionType,entityType,entityId,ipAddress,createdAt\n");
            for (AuditTrailPort.AuditLogEntry e : entries) {
                sw.write(String.join(",",
                        safe(e.id() != null ? e.id().toString() : ""),
                        safe(e.actorFullName()),
                        safe(e.actorEmail()),
                        safe(e.actionType()),
                        safe(e.entityType()),
                        safe(e.entityId() != null ? e.entityId().toString() : ""),
                        safe(e.ipAddress()),
                        safe(e.createdAt() != null ? e.createdAt().format(CSV_TS_FORMAT) : "")));
                sw.write("\n");
            }
            csv = sw.toString();
        }

        byte[] bytes = csv.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-trail.csv\"");
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    @PostMapping("/exports")
    public ResponseEntity<Map<String, Object>> queueExport(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer linkExpiryHours) {
        if (principal == null || principal.tenantSlug() == null || principal.tenantSlug().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Tenant context is required.");
        }
        LocalDate effectiveTo = to == null ? LocalDate.now() : to;
        LocalDate effectiveFrom = from == null ? effectiveTo.minusDays(30) : from;
        var requestId = auditExportService.queueExport(
                principal.userId(),
                principal.email(),
                principal.tenantSlug(),
                actorEmail,
                actionType,
                entityType,
                effectiveFrom,
                effectiveTo,
                linkExpiryHours);
        Map<String, Object> response = Map.of(
                "requestId", requestId,
                "status", "PENDING",
                "message", "Export queued and will be sent to your email.");
        return ResponseEntity.accepted().body(response);
    }

    @GetMapping("/exports/download/{token}")
    @PreAuthorize("permitAll()")
    public ResponseEntity<byte[]> downloadExport(@PathVariable String token) throws IOException {
        AuditExportRequestEntity request = auditExportService.getByTokenOrThrow(token);
        Path filePath = Path.of(request.getFileStoragePath()).toAbsolutePath().normalize();
        if (!Files.exists(filePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Export file no longer exists.");
        }
        byte[] bytes = Files.readAllBytes(filePath);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + request.getFileName() + "\"");
        headers.setContentType(MediaType.parseMediaType("application/gzip"));
        return new ResponseEntity<>(bytes, headers, HttpStatus.OK);
    }

    /**
     * Escapes a CSV field: wraps in quotes if it contains comma, quote, or newline.
     */
    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
