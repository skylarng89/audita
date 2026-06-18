package io.audita.api.controller;

import io.audita.api.dto.response.TenantAdminSettingsResponse;
import io.audita.api.security.UserPrincipal;
import io.audita.application.port.SampleDataPort;
import io.audita.application.port.TenantSettingsPort;
import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.model.ApprovalType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/settings")
public class TenantSettingsController {

        private final TenantSettingsPort tenantSettingsPort;
        private final SampleDataPort sampleDataPort;

        @Value("${audita.jwt.expiry-seconds:900}")
        private int jwtExpirySeconds;

        public TenantSettingsController(TenantSettingsPort tenantSettingsPort, SampleDataPort sampleDataPort) {
                this.tenantSettingsPort = tenantSettingsPort;
                this.sampleDataPort = sampleDataPort;
        }

        @GetMapping
        @PreAuthorize("@authz.hasPermission(authentication, 'settings.view')")
        public TenantAdminSettingsResponse getSettings(@AuthenticationPrincipal UserPrincipal principal) {
                String tenantSlug = principal == null ? null : principal.tenantSlug();
                if (tenantSlug == null || tenantSlug.isBlank()) {
                        throw new DomainNotPermittedException("TENANT_CONTEXT_REQUIRED", "Tenant context is required.");
                }

                TenantSettingsPort.TenantSettings settings = tenantSettingsPort.getTenantSettings(tenantSlug);
                TenantSettingsPort.TenantProfile tenant = settings.profile();

                TenantAdminSettingsResponse.OrganizationProfile profile = new TenantAdminSettingsResponse.OrganizationProfile(
                                tenant.name(),
                                tenant.slug(),
                                tenant.subdomain(),
                                tenant.primaryContactEmail(),
                                tenant.timezone(),
                                tenant.status());

                TenantAdminSettingsResponse.FeatureFlags featureFlags = new TenantAdminSettingsResponse.FeatureFlags(
                                false, false, false);

                TenantAdminSettingsResponse.SecurityDefaults securityDefaults = new TenantAdminSettingsResponse.SecurityDefaults(
                                Math.max(1, jwtExpirySeconds / 60),
                                "Not configured",
                                "Minimum 12 chars with upper, lower, number, and symbol");

                TenantAdminSettingsResponse.WorkflowDefaults workflowDefaults = new TenantAdminSettingsResponse.WorkflowDefaults(
                                settings.workflowDefaults().approvalTypeDefault(),
                                settings.workflowDefaults().requireDefaultApprovers());

                TenantAdminSettingsResponse.SlaDefaults slaDefaults = new TenantAdminSettingsResponse.SlaDefaults(
                                settings.slaDefaults().lowHours(),
                                settings.slaDefaults().mediumHours(),
                                settings.slaDefaults().highHours(),
                                settings.slaDefaults().criticalHours(),
                                settings.slaDefaults().warningBeforeHours());

                TenantAdminSettingsResponse.AutoApproverDefaults autoApproverDefaults = new TenantAdminSettingsResponse.AutoApproverDefaults(
                                settings.autoApproverDefaults().userIds(),
                                settings.autoApproverDefaults().groupIds());

TenantAdminSettingsResponse.AuditDefaults auditDefaults = new TenantAdminSettingsResponse.AuditDefaults(
                        settings.auditDefaults().exportLinkExpiryHours());

                boolean sampleDataImported = sampleDataPort.isSampleDataImported(tenantSlug);

                return new TenantAdminSettingsResponse(
                        profile,
                        featureFlags,
                        securityDefaults,
                        workflowDefaults,
                        slaDefaults,
                        autoApproverDefaults,
                        auditDefaults,
                        sampleDataImported);
        }

        @PatchMapping
        @PreAuthorize("@authz.hasPermission(authentication, 'settings.manage')")
        public TenantAdminSettingsResponse patchSettings(@AuthenticationPrincipal UserPrincipal principal,
                        @RequestBody Map<String, Object> body) {
                String tenantSlug = principal == null ? null : principal.tenantSlug();
                if (tenantSlug == null || tenantSlug.isBlank()) {
                        throw new DomainNotPermittedException("TENANT_CONTEXT_REQUIRED", "Tenant context is required.");
                }

                Map<String, Object> profile = requireObject(body, "profile");
                String name = requireText(profile, "name");
                String subdomain = optionalText(profile, "subdomain");
                String primaryContactEmail = optionalText(profile, "primaryContactEmail");
                String timezone = requireText(profile, "timezone");

                Map<String, Object> workflowDefaults = requireObject(body, "workflowDefaults");
                ApprovalType approvalTypeDefault = parseApprovalType(requireText(workflowDefaults, "approvalTypeDefault"));
                boolean requireDefaultApprovers = requireBoolean(workflowDefaults, "requireDefaultApprovers");

                Map<String, Object> slaDefaults = requireObject(body, "slaDefaults");
                int lowHours = requireInt(slaDefaults, "lowHours");
                int mediumHours = requireInt(slaDefaults, "mediumHours");
                int highHours = requireInt(slaDefaults, "highHours");
                int criticalHours = requireInt(slaDefaults, "criticalHours");
                int warningBeforeHours = requireInt(slaDefaults, "warningBeforeHours");

                Map<String, Object> autoApproverDefaults = optionalObject(body, "autoApproverDefaults");
                List<UUID> autoApproverUserIds = parseUuidList(readStringList(autoApproverDefaults, "userIds"), "userIds");
                List<UUID> autoApproverGroupIds = parseUuidList(readStringList(autoApproverDefaults, "groupIds"), "groupIds");

                Map<String, Object> auditDefaults = optionalObject(body, "auditDefaults");
                int exportLinkExpiryHours = optionalInt(auditDefaults, "exportLinkExpiryHours", 24);

                int minDeadline = Math.min(
                                Math.min(lowHours, mediumHours),
                                Math.min(highHours, criticalHours));
                if (warningBeforeHours >= minDeadline) {
                        throw new ResponseStatusException(
                                        HttpStatus.BAD_REQUEST,
                                        "warningBeforeHours must be lower than all SLA deadline hour values.");
                }

                tenantSettingsPort.updateWorkflowDefaults(
                                tenantSlug,
                                new TenantSettingsPort.WorkflowDefaults(
                                                approvalTypeDefault,
                                                requireDefaultApprovers));
                tenantSettingsPort.updateProfile(
                                tenantSlug,
                                new TenantSettingsPort.ProfileUpdate(
                                                name,
                                                subdomain,
                                                primaryContactEmail,
                                                timezone));
                tenantSettingsPort.updateSlaDefaults(
                                tenantSlug,
                                new TenantSettingsPort.SlaDefaults(
                                                lowHours,
                                                mediumHours,
                                                highHours,
                                                criticalHours,
                                                warningBeforeHours));
                tenantSettingsPort.updateAutoApproverDefaults(
                                tenantSlug,
                                new TenantSettingsPort.AutoApproverDefaults(
                                                autoApproverUserIds,
                                                autoApproverGroupIds));
                tenantSettingsPort.updateAuditDefaults(
                                tenantSlug,
                                new TenantSettingsPort.AuditDefaults(exportLinkExpiryHours));

                return getSettings(principal);
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> requireObject(Map<String, Object> source, String fieldName) {
                Object raw = source.get(fieldName);
                if (raw instanceof Map<?, ?> rawMap) {
                        return (Map<String, Object>) rawMap;
                }
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing or invalid object: " + fieldName);
        }

        @SuppressWarnings("unchecked")
        private Map<String, Object> optionalObject(Map<String, Object> source, String fieldName) {
                Object raw = source.get(fieldName);
                if (raw == null) {
                        return Collections.emptyMap();
                }
                if (raw instanceof Map<?, ?> rawMap) {
                        return (Map<String, Object>) rawMap;
                }
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid object: " + fieldName);
        }

        private String requireText(Map<String, Object> source, String fieldName) {
                Object raw = source.get(fieldName);
                if (raw == null) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing field: " + fieldName);
                }
                String value = raw.toString().trim();
                if (value.isEmpty()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field cannot be blank: " + fieldName);
                }
                return value;
        }

        private String optionalText(Map<String, Object> source, String fieldName) {
                Object raw = source.get(fieldName);
                if (raw == null) {
                        return null;
                }
                String value = raw.toString().trim();
                return value.isEmpty() ? null : value;
        }

        private boolean requireBoolean(Map<String, Object> source, String fieldName) {
                Object raw = source.get(fieldName);
                if (raw instanceof Boolean boolValue) {
                        return boolValue;
                }
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field must be boolean: " + fieldName);
        }

        private int requireInt(Map<String, Object> source, String fieldName) {
                Object raw = source.get(fieldName);
                if (raw instanceof Number number) {
                        return number.intValue();
                }
                if (raw instanceof String stringValue) {
                        try {
                                return Integer.parseInt(stringValue.trim());
                        } catch (NumberFormatException ignored) {
                        }
                }
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Field must be an integer: " + fieldName);
        }

        private int optionalInt(Map<String, Object> source, String fieldName, int fallback) {
                if (!source.containsKey(fieldName)) {
                        return fallback;
                }
                return requireInt(source, fieldName);
        }

        private ApprovalType parseApprovalType(String approvalType) {
                try {
                        return ApprovalType.valueOf(approvalType.trim().toUpperCase());
                } catch (IllegalArgumentException ex) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Invalid approvalTypeDefault: " + approvalType);
                }
        }

        private List<String> readStringList(Map<String, Object> source, String fieldName) {
                Object raw = source.get(fieldName);
                if (raw == null) {
                        return List.of();
                }
                if (!(raw instanceof List<?> rawList)) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "Field must be an array: autoApproverDefaults." + fieldName);
                }
                return rawList.stream().map(item -> item == null ? "" : item.toString()).toList();
        }

        private List<UUID> parseUuidList(List<String> ids, String fieldName) {
                return ids.stream()
                                .map(id -> id == null ? "" : id.trim())
                                .filter(id -> !id.isEmpty())
                                .map(id -> {
                                        try {
                                                return UUID.fromString(id);
                                        } catch (IllegalArgumentException ex) {
                                                throw new ResponseStatusException(
                                                                HttpStatus.BAD_REQUEST,
                                                                "Invalid UUID in autoApproverDefaults." + fieldName + ": " + id);
                                        }
                                })
                                .distinct()
                                .toList();
        }
}
