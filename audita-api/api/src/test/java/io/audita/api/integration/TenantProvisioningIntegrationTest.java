package io.audita.api.integration;

import io.audita.infrastructure.persistence.repository.TenantRepository;
import io.audita.infrastructure.persistence.repository.UserRepository;
import io.audita.infrastructure.service.EmailService;
import io.audita.infrastructure.service.TenantService;
import io.audita.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TenantProvisioningIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("audita_test")
            .withUsername("audita")
            .withPassword("secret");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("audita.encryption.key", () -> "0000000000000000000000000000000000000000000000000000000000000000");
        registry.add("spring.flyway.enabled", () -> true);
    }

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private EmailService emailService;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void provisionCreatesTenantSchemaAndKeepsUsersIsolatedPerTenant() {
        tenantService.provision("Alpha Org", "alphaorg", "admin.alpha@example.com", "Alpha Admin");
        tenantService.provision("Beta Org", "betaorg", "admin.beta@example.com", "Beta Admin");

        assertThat(tenantRepository.findBySlug("alphaorg")).isPresent();
        assertThat(tenantRepository.findBySlug("betaorg")).isPresent();

        TenantContext.setCurrentTenant("alphaorg");
        assertThat(userRepository.findByEmail("admin.alpha@example.com")).isPresent();
        assertThat(userRepository.findByEmail("admin.beta@example.com")).isNotPresent();

        TenantContext.setCurrentTenant("betaorg");
        assertThat(userRepository.findByEmail("admin.beta@example.com")).isPresent();
        assertThat(userRepository.findByEmail("admin.alpha@example.com")).isNotPresent();
    }
}
