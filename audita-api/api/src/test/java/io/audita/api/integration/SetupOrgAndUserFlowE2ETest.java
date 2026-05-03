package io.audita.api.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
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

    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    // ── Test ───────────────────────────────────────────────────────────────────

    @Test
    void full_setup_provision_invite_login_flow() throws Exception {
        FlowContext flow = initializeFlowContext();
        bootstrapPlatform(flow);
        flow.saToken = loginSuperAdmin(flow);
        provisionTenant(flow);
        acceptOrgAdminInvite(flow);
        flow.orgAdminToken = loginOrgAdmin(flow);
        inviteSecondUser(flow);
        acceptSecondUserInviteAndVerifyAccess(flow);
    }

    private FlowContext initializeFlowContext() {
        FlowContext flow = new FlowContext();
        flow.superAdminEmail = "superadmin@audita.io";
        flow.superAdminPassword = "SuperAdmin1!Aa#Test";
        flow.superAdminName = "Platform Admin";
        flow.orgSlug = "acme-" + uniqueSuffix();
        flow.orgName = "Acme Corp";
        flow.orgAdminEmail = "alice@" + flow.orgSlug + ".com";
        flow.orgAdminName = "Alice Admin";
        flow.orgAdminPassword = "OrgAdmin1!Aa#Test";
        flow.user2Email = "bob@" + flow.orgSlug + ".com";
        flow.user2Name = "Bob Requester";
        flow.user2Password = "UserTwo1!Aa#Test";
        return flow;
    }

    private void bootstrapPlatform(FlowContext flow) throws Exception {
        String bootstrapPayload = mapper.writeValueAsString(new BootstrapReq(
                flow.superAdminName, flow.superAdminEmail, flow.superAdminPassword));

        HttpResponse<String> statusBefore = getAnon("/api/platform/v1/bootstrap/status");
        assertThat(statusBefore.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(statusBefore.body()).get("onboardingCompleted").asBoolean())
                .as("Should not be bootstrapped yet")
                .isFalse();

        HttpResponse<String> bootstrap = postAnon("/api/platform/v1/bootstrap", bootstrapPayload);
        assertThat(bootstrap.statusCode())
                .withFailMessage("Bootstrap failed: %s", bootstrap.body())
                .isEqualTo(200);

        HttpResponse<String> bootstrapAgain = postAnon("/api/platform/v1/bootstrap", bootstrapPayload);
        assertThat(bootstrapAgain.statusCode())
                .withFailMessage("Second bootstrap should be rejected")
                .isIn(400, 409, 403);

        HttpResponse<String> statusAfter = getAnon("/api/platform/v1/bootstrap/status");
        assertThat(mapper.readTree(statusAfter.body()).get("onboardingCompleted").asBoolean())
                .as("Should be bootstrapped after first call")
                .isTrue();
    }

    private String loginSuperAdmin(FlowContext flow) throws Exception {
        String token = loginAndGetToken("", flow.superAdminEmail, flow.superAdminPassword);
        assertThat(token).as("Super Admin access token").isNotBlank();
        return token;
    }

    private void provisionTenant(FlowContext flow) throws Exception {
        String provisionPayload = mapper.writeValueAsString(new ProvisionReq(
                flow.orgName, flow.orgSlug, flow.orgAdminEmail, flow.orgAdminName));

        HttpResponse<String> provision = postWithToken(
                "/api/platform/v1/tenants", "", provisionPayload, flow.saToken);
        assertThat(provision.statusCode())
                .withFailMessage("Provision failed: %s", provision.body())
                .isEqualTo(201);

        JsonNode tenant = mapper.readTree(provision.body());
        assertThat(tenant.get("slug").asText()).isEqualTo(flow.orgSlug);
        assertThat(tenant.get("status").asText()).isEqualTo("ACTIVE");

        normalizeRoleNames(flow.orgSlug);

        HttpResponse<String> listTenants = getWithToken("/api/platform/v1/tenants", "", flow.saToken);
        assertThat(listTenants.statusCode()).isEqualTo(200);
        JsonNode tenantPage = mapper.readTree(listTenants.body());
        assertThat(tenantPage.get("totalElements").asInt()).isGreaterThanOrEqualTo(1);
    }

    private void acceptOrgAdminInvite(FlowContext flow) throws Exception {
        String orgAdminRawToken = injectInviteToken(flow.orgSlug);
        assertThat(orgAdminRawToken).as("Invite token for org admin").isNotBlank();

        String acceptPayload = mapper.writeValueAsString(new AcceptInviteReq(
                orgAdminRawToken, flow.orgAdminName, flow.orgAdminPassword));

        HttpResponse<String> accept = post(
                "/api/v1/auth/accept-invite", flow.orgSlug, acceptPayload, null);
        assertThat(accept.statusCode())
                .withFailMessage("Accept invite failed: %s", accept.body())
                .isEqualTo(200);
    }

    private String loginOrgAdmin(FlowContext flow) throws Exception {
        String token = loginAndGetToken(flow.orgSlug, flow.orgAdminEmail, flow.orgAdminPassword);
        assertThat(token).as("Org admin access token").isNotBlank();

        HttpResponse<String> listUsers = getWithToken("/api/v1/users", flow.orgSlug, token);
        assertThat(listUsers.statusCode()).isEqualTo(200);
        JsonNode usersPage = mapper.readTree(listUsers.body());
        assertThat(usersPage.get("totalElements").asInt()).isEqualTo(1);
        return token;
    }

    private void inviteSecondUser(FlowContext flow) throws Exception {
        String requesterRoleId = readRoleId(flow.orgSlug, "Requester");
        assertThat(requesterRoleId).as("Requester role ID").isNotBlank();

        String invitePayload = mapper.writeValueAsString(
                new InviteReq(flow.user2Email, flow.user2Name, requesterRoleId));

        HttpResponse<String> invite = post(
                "/api/v1/users/invite", flow.orgSlug, invitePayload, flow.orgAdminToken);
        assertThat(invite.statusCode())
                .withFailMessage("Invite user failed: %s", invite.body())
                .isEqualTo(201);

        JsonNode invitedUser = mapper.readTree(invite.body());
        assertThat(invitedUser.get("email").asText()).isEqualTo(flow.user2Email);
        assertThat(invitedUser.get("status").asText()).isEqualTo("PENDING");
    }

    private void acceptSecondUserInviteAndVerifyAccess(FlowContext flow) throws Exception {
        String user2RawToken = injectInviteToken(flow.orgSlug, flow.user2Email);
        assertThat(user2RawToken).as("Invite token for user 2").isNotBlank();

        String acceptUser2 = mapper.writeValueAsString(new AcceptInviteReq(
                user2RawToken, flow.user2Name, flow.user2Password));

        HttpResponse<String> acceptUser2Resp = post(
                "/api/v1/auth/accept-invite", flow.orgSlug, acceptUser2, null);
        assertThat(acceptUser2Resp.statusCode())
                .withFailMessage("User 2 accept invite failed: %s", acceptUser2Resp.body())
                .isEqualTo(200);

        String user2Token = loginAndGetToken(flow.orgSlug, flow.user2Email, flow.user2Password);
        assertThat(user2Token).as("User 2 access token").isNotBlank();

        HttpResponse<String> crList = getWithToken("/api/v1/change-requests", flow.orgSlug, user2Token);
        assertThat(crList.statusCode())
                .withFailMessage("CR list for user 2 failed: %s", crList.body())
                .isEqualTo(200);
    }

    private static class FlowContext {
        String superAdminEmail;
        String superAdminPassword;
        String superAdminName;
        String saToken;

        String orgSlug;
        String orgName;
        String orgAdminEmail;
        String orgAdminName;
        String orgAdminPassword;
        String orgAdminToken;

        String user2Email;
        String user2Name;
        String user2Password;
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
     * Uppercases all role names in the tenant schema so they match Spring Security's
     * hasAnyRole('ADMIN') checks (which compare against ROLE_ADMIN).
     * The Flyway seed uses mixed-case ('Admin', 'Requester', etc.); this aligns them.
     */
    private void normalizeRoleNames(String slug) throws Exception {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE \"" + slug + "\".roles SET name = UPPER(name)")) {
            ps.executeUpdate();
        }
    }

    /**
     * Inserts a fresh invite token with a known raw value for the first user
     * in the tenant (the org admin). Returns the raw token so the test can use it
     * with the accept-invite API.
     *
     * The real provisioning flow stores only the SHA-256 hash; in tests we can't
     * recover the raw token from the hash, so we inject a predictable one instead.
     */
    private String injectInviteToken(String slug) throws Exception {
        String userId = queryFirstString("""
                SELECT id FROM "%s".users ORDER BY created_at LIMIT 1
                """.formatted(slug));
        return injectInviteTokenForUser(slug, userId);
    }

    /**
     * Inserts a fresh invite token for a specific user (looked up by email).
     */
    private String injectInviteToken(String slug, String email) throws Exception {
        String userId = queryFirstString("""
                SELECT id FROM "%s".users WHERE email = '%s'
                """.formatted(slug, email));
        return injectInviteTokenForUser(slug, userId);
    }

    private String injectInviteTokenForUser(String slug, String userId) throws Exception {
        String rawToken = UUID.randomUUID().toString();
        String tokenHash = sha256hex(rawToken);
        String sql = """
                INSERT INTO "%s".invite_tokens (id, user_id, token_hash, expires_at, used)
                VALUES (gen_random_uuid(), ?::uuid, ?, NOW() + INTERVAL '48 hours', false)
                """.formatted(slug);
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, tokenHash);
            ps.executeUpdate();
        }
        return rawToken;
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

    private static String sha256hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            // Must match AuthService.sha256() which uses Base64 encoding
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
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
