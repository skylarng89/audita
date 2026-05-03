package io.audita.api.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class TenantSchemaIsolationIntegrationTest {

    @Container
    @SuppressWarnings("resource")
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("audita_test")
            .withUsername("audita")
            .withPassword("secret");

    @Test
    void tenantSchemasAreMigratedAndIsolated() throws Exception {
        migratePublic();
        migrateTenant("tenant_alpha");
        migrateTenant("tenant_beta");

        try (Connection c = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            insertUser(c, "tenant_alpha", "alpha@example.com", "Alpha User");
            insertUser(c, "tenant_beta", "beta@example.com", "Beta User");

            assertThat(countUsers(c, "tenant_alpha", "alpha@example.com")).isEqualTo(1);
            assertThat(countUsers(c, "tenant_alpha", "beta@example.com")).isZero();
            assertThat(countUsers(c, "tenant_beta", "beta@example.com")).isEqualTo(1);
            assertThat(countUsers(c, "tenant_beta", "alpha@example.com")).isZero();
        }
    }

    private void migratePublic() {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas("public")
                .locations("classpath:db/migration/public")
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .load()
                .migrate();
    }

    private void migrateTenant(String schema) {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas(schema)
                .locations("classpath:db/migration/tenant")
                .table("flyway_schema_history")
                .baselineOnMigrate(false)
                .validateOnMigrate(true)
                .load()
                .migrate();
    }

    private void insertUser(Connection c, String schema, String email, String fullName) throws Exception {
        UUID roleId = findRoleId(c, schema, "Admin");

        String sql = "INSERT INTO " + schema + ".users (id, email, full_name, role_id, status) VALUES (?, ?, ?, ?, 'ACTIVE')";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setString(2, email);
            ps.setString(3, fullName);
            ps.setObject(4, roleId);
            ps.executeUpdate();
        }
    }

    private UUID findRoleId(Connection c, String schema, String roleName) throws Exception {
        String sql = "SELECT id FROM " + schema + ".roles WHERE name = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, roleName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject(1, UUID.class);
                }
            }
        }
        throw new IllegalStateException("Expected role not found in schema " + schema + ": " + roleName);
    }

    private int countUsers(Connection c, String schema, String email) throws Exception {
        String sql = "SELECT count(*) FROM " + schema + ".users WHERE email = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
