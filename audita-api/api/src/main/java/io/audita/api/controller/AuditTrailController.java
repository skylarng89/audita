package io.audita.api.controller;

import io.audita.api.dto.response.AuditLogResponse;
import io.audita.api.dto.response.PageResponse;
import io.audita.application.port.AuditTrailPort;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit-trail")
@PreAuthorize("hasAnyRole('ADMIN', 'AUDITOR', 'SUPER_ADMIN')")
public class AuditTrailController {

    private static final DateTimeFormatter CSV_TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final AuditTrailPort auditTrailPort;

    public AuditTrailController(AuditTrailPort auditTrailPort) {
        this.auditTrailPort = auditTrailPort;
    }

    @GetMapping
    public PageResponse<AuditLogResponse> query(
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        return PageResponse.from(
                auditTrailPort.query(actorEmail, actionType, entityType, from, to, pageable),
                AuditLogResponse::from
        );
    }

    @GetMapping(value = "/export.csv", produces = "text/csv")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam(required = false) String actorEmail,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to)
            throws IOException {

        List<AuditTrailPort.AuditLogEntry> entries =
                auditTrailPort.export(actorEmail, actionType, entityType, from, to);

        StringWriter sw = new StringWriter();
        sw.write("id,actorEmail,actionType,entityType,entityId,ipAddress,createdAt\n");
        for (AuditTrailPort.AuditLogEntry e : entries) {
            sw.write(String.join(",",
                    safe(e.id() != null ? e.id().toString() : ""),
                    safe(e.actorEmail()),
                    safe(e.actionType()),
                    safe(e.entityType()),
                    safe(e.entityId() != null ? e.entityId().toString() : ""),
                    safe(e.ipAddress()),
                    safe(e.createdAt() != null ? e.createdAt().format(CSV_TS_FORMAT) : "")
            ));
            sw.write("\n");
        }

        byte[] bytes = sw.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-trail.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(bytes);
    }

    /** Escapes a CSV field: wraps in quotes if it contains comma, quote, or newline. */
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
