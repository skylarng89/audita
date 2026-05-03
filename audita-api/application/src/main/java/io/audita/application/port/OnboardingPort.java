package io.audita.application.port;

public interface OnboardingPort {

    String findFirstTenantSlug();

    void setupSingleTenant(String orgName,
                           String slug,
                           String adminFullName,
                           String adminEmail,
                           String rawPassword);
}