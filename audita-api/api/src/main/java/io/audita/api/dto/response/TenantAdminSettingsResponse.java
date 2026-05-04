package io.audita.api.dto.response;

public record TenantAdminSettingsResponse(
        OrganizationProfile profile,
        FeatureFlags featureFlags,
        SecurityDefaults securityDefaults
) {
    public record OrganizationProfile(
            String name,
            String slug,
            String primaryContactEmail,
            String timezone,
            String status
    ) {}

    public record FeatureFlags(
            boolean policyBreachDigests,
            boolean automatedReminders,
            boolean conditionalEscalation
    ) {}

    public record SecurityDefaults(
            Integer sessionTimeoutMinutes,
            String mfaPolicy,
            String passwordPolicy
    ) {}
}