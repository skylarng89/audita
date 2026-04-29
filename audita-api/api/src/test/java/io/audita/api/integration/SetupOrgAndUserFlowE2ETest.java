package io.audita.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Layer 1 E2E — full setup flow:
 *
 *  1. Bootstrap platform  (POST /api/platform/v1/bootstrap)
 *  2. Super Admin logs in (POST /api/v1/auth/login  — no tenant slug)
 *  3. Provision org       (POST /api/platform/v1/tenants)
 *  4. Retrieve invite token from DB and accept invite for org admin
 *     (POST /api/v1/auth/accept-invite)
 *  5. Org admin logs in   (POST /api/v1/auth/login with tenant slug)
 *  6. Org admin invites a second user  (POST /api/v1/users/invite)
 *  7. Second user accepts invite and logs in
 *
 * The test uses Testcontainers, runs with an embedded Spring Boot server,
 * and does NOT share state with other tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.task.scheduling.enabled=false"})
@Tag("Layer1")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SetupOrgAndUserFlowE2ETest {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("audita_setup_e2e")
            .withUsername("audita")
            .withPassword("secret");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    int port;

    @Autowired
    PasswordEncoder passwordEncoder;

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    // ── Test ───────────────────────────────────────────────────────────────────

    @Test
    void full_setup_provision_invite_login_flow() throws Exception {

        // ── 1. Bootstrap platform ─────────────────────────────────────────────

        String superAdminEmail    = "superadmin@audita.io";
        String superAdminPassword = "SuperAdmin1!Aa";
        String superAdminName     = "Platform Admin";

        String bootstrapPayload = mapper.writeValueAsString(new BootstrapReq(
                superAdminName, superAdminEmail, superAdminPassword));

        // Verify status is not yet bootstrapped
        HttpResponse<String> statusBefore = getAnon("/api/platform/v1/bootstrap/status");
        assertThat(statusBefore.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(statusBefore.body()).get("onboardingCompleted").asBoolean())
                .as("Should not be bootstrapped yet").isFalse();

        HttpResponse<String> bootstrap = postAnon("/api/platform/v1/bootstrap", bootstrapPayload);
        assertThat(bootstrap.statusCode())
                .withFailMessage("Bootstrap failed: %s", bootstrap.body())
                .isEqualTo(200);

        // Idempotency: second call must fail (already bootstrapped)
        HttpResponse<String> bootstrapAgain = postAnon("/api/platform/v1/bootstrap", bootstrapPayload);
        assertThat(bootstrapAgain.statusCode())
                .withFailMessage("Second bootstrap should be rejected")
                .isIn(400, 409, 403);

        // Status should now be true
        HttpResponse<String> statusAfter = getAnon("/api/platform/v1/bootstrap/status");
        assertThat(mapper.readTree(statusAfter.body()).get("onboardingCompleted").asBoolean())
                .as("Should be bootstrapped after first call").isTrue();

        // ── 2. Super Admin login ──────────────────────────────────────────────

        // Super Admin has no tenant slug — use empty string
        String saToken = loginAndGetToken("", superAdminEmail, superAdminPassword);
        assertThat(saToken).as("Super Admin access token").isNotBlank();

        // ── 3. Provision organisation ─────────────────────────────────────────

        String orgSlug      = "acme-" + uniqueSuffix();
        String orgName      = "Acme Corp";
        String orgAdminEmail = "alice@" + orgSlug + ".com";
        String orgAdminName  = "Alice Admin";

        String provisionPayload = mapper.writeValueAsString(new ProvisionReq(
                orgName, orgSlug, orgAdminEmail, orgAdminName));

        HttpResponse<String> provision = postWithToken(
                "/api/platform/v1/tenants", "", provisionPayload, saToken);
        assertThat(provision.statusCode())
                .withFailMessage("Provision failed: %s", provision.body())
                .isEqualTo(201);

        JsonNode tenant = mapper.readTree(provision.body());
        assertThat(tenant.get("slug").asText()).isEqualTo(orgSlug);
        assertThat(tenant.get("status").asText()).isEqualTo("ACTIVE");
        String tenantId = tenant.get("id").asText();

        // Super Admin can list tenants and see the new org
        HttpResponse<String> listTenants = getWithToken("/api/platform/v1/tenants", "", saToken);
        assertThat(listTenants.statusCode()).isEqualTo(200);
        JsonNode tenantPage = mapper.readTree(listTenants.body());
        assertThat(tenantPage.get("totalElements").asInt()).isGreaterThanOrEqualTo(1);

        // ── 4. Accept invite — org admin sets password ─────────────────────────

        // Provisioning sends an invite email but we read the raw token from DB in tests
        String orgAdminRawToken = readInviteToken(orgSlug);
        assertThat(orgAdminRawToken).as("Invite token for org admin").isNotBlank();

        String orgAdminPassword = "OrgAdmin1!Aa";
        String acceptPayload = mapper.writeValueAsString(new AcceptInviteReq(
                orgAdminRawToken, orgAdminName, orgAdminPassword));

        HttpResponse<String> accept = post(
                "/api/v1/auth/accept-invite", orgSlug, acceptPayload, null);
        assertThat(accept.statusCode())
                .withFailMessage("Accept invite failed: %s", accept.body())
                .isEqualTo(200);

        // ── 5. Org admin logs in ──────────────────────────────────────────────

        String orgAdminToken = loginAndGetToken(orgSlug, orgAdminEmail, orgAdminPassword);
        assertThat(orgAdminToken).as("Org admin access token").isNotBlank();

        // Org admin can list users in their tenant (only themselves so far)
        HttpResponse<String> listUsers = getWithToken("/api/v1/users", orgSlug, orgAdminToken);
        assertThat(listUsers.statusCode()).isEqualTo(200);
        JsonNode usersPage = mapper.readTree(listUsers.body());
        assertThat(usersPage.get("totalElements").asInt()).isEqualTo(1);

        // ── 6. Org admin invites a second user ────────────────────────────────

        // Read Requester role ID directly from DB (avoids Hibernate camelCase column mapping bug)
        String requesterRoleId = readRoleId(orgSlug, "Requester");
        assertThat(requesterRoleId).as("Requester role ID").isNotBlank();

        String user2Email = "bob@" + orgSlug + ".com";
        String user2Name  = "Bob Requester";

        String invitePayload = mapper.writeValueAsString(
                new InviteReq(user2Email, user2Name, requesterRoleId));

        HttpResponse<String> invite = post(
                "/api/v1/users/invite", orgSlug, invitePayload, orgAdminToken);
        assertThat(invite.statusCode())
                .withFailMessage("Invite user failed: %s", invite.body())
                .isEqualTo(201);

        JsonNode invitedUser = mapper.readTree(invite.body());
        assertThat(invitedUser.get("email").asText()).isEqualTo(user2Email);
        assertThat(invitedUser.get("status").asText()).isEqualTo("PENDING");

        // ── 7. Second user accepts invite and logs in ─────────────────────────

        String user2RawToken = readInviteToken(orgSlug, user2Email);
        assertThat(user2RawToken).as("Invite token for user 2").isNotBlank();

        String user2Password = "UserTwo1!Aa";
        String acceptUser2 = mapper.writeValueAsString(new AcceptInviteReq(
                user2RawToken, user2Name, user2Password));

        HttpResponse<String> acceptUser2Resp = post(
                "/api/v1/auth/accept-invite", orgSlug, acceptUser2, null);
        assertThat(acceptUser2Resp.statusCode())
                .withFailMessage("User 2 accept invite failed: %s", acceptUser2Resp.body())
                .isEqualTo(200);

        String user2Token = loginAndGetToken(orgSlug, user2Email, user2Password);
        assertThat(user2Token).as("User 2 access token").isNotBlank();

        // User 2 (Requester role) can see the change requests list
        HttpResponse<String> crList = getWithToken("/api/v1/change-requests", orgSlug, user2Token);
        assertThat(crList.statusCode())
                .withFailMessage("CR list for user 2 failed: %s", crList.body())
                .isEqualTo(200);
    }

    // ── HTTP helpers ───────────────────────────────────────────────────────────

    private HttpResponse<String> postAnon(String path, String body) throws Exception {
        return post(path, null, body, null);
    }

    private HttpResponse<String> getAnon(String path) throws Exception {
        return http.send(HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postWithToken(String path, String slug, String body, String token)
            throws Exception {
        return post(path, slug, body, token);
    }

    private HttpResponse<String> post(String path, String slug, String body, String token)
            throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (slug != null) b.header("X-Tenant-Slug", slug);
        if (token != null) b.header("Authorization", "Bearer " + token);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> getWithToken(String path, String slug, String token)
            throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET();
        if (slug != null) b.header("X-Tenant-Slug", slug);
        if (token != null) b.header("Authorization", "Bearer " + token);
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String loginAndGetToken(String slug, String email, String password) throws Exception {
        String payload = mapper.writeValueAsString(new LoginReq(email, password));
        HttpResponse<String> resp = post("/api/v1/auth/login", slug, payload, null);
        assertThat(resp.statusCode())
                .withFailMessage("Login failed for %s: status=%s body=%s", email, resp.statusCode(), resp.body())
                .isEqualTo(200);
        return mapper.readTree(resp.body()).get("accessToken").asText();
    }

    // ── DB helpers ─────────────────────────────────────────────────────────────

    /**
     * Reads the most-recently-created raw invite token for the first PENDING user
     * in the tenant schema (the org admin).
     */
    private String readInviteToken(String slug) throws Exception {
        String sql = """
                SELECT it.raw_token
                FROM "%s".invite_tokens it
                JOIN "%s".users u ON it.user_id = u.id
                ORDER BY it.created_at DESC
                LIMIT 1
                """.formatted(slug, slug);
        return queryFirstString(sql);
    }

    /**
     * Reads the invite token for a specific user email within the tenant schema.
     */
    private String readInviteToken(String slug, String email) throws Exception {
        String sql = """
                SELECT it.raw_token
                FROM "%s".invite_tokens it
                JOIN "%s".users u ON it.user_id = u.id
                WHERE u.email = ?
                ORDER BY it.created_at DESC
                LIMIT 1
                """.formatted(slug, slug);

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).as("Invite token row for %s", email).isTrue();
                return rs.getString(1);
            }
        }
    }

    private String queryFirstString(String sql) throws Exception {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            assertThat(rs.next()).as("Expected a row for query: %s", sql).isTrue();
            return rs.getString(1);
        }
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private String readRoleId(String slug, String roleName) throws Exception {
        String sql = """
                SELECT id FROM "%s".roles WHERE LOWER(name) = LOWER(?)
                """.formatted(slug);
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, roleName);
            try (ResultSet rs = ps.executeQuery()) {
                assertThat(rs.next()).as("Role '%s' in schema %s", roleName, slug).isTrue();
                return rs.getString(1);
            }
        }
    }

    private String findRoleId(JsonNode roles, String roleName) {
        // Roles might be a plain array or a Page wrapper
        JsonNode content = roles.isArray() ? roles : roles.path("content");
        for (JsonNode role : content) {
            if (roleName.equalsIgnoreCase(role.path("name").asText())) {
                return role.get("id").asText();
            }
        }
        return "";
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    // ── Request/response payload records ──────────────────────────────────────

    record BootstrapReq(String fullName, String email, String password) {}
    record LoginReq(String email, String password) {}
    record ProvisionReq(String name, String slug, String adminEmail, String adminFullName) {}
    record AcceptInviteReq(String token, String fullName, String password) {}
    record InviteReq(String email, String fullName, String roleId) {}
}
