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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"spring.task.scheduling.enabled=false"})
@Tag("Layer1")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CriticalFlowsE2EL1Test {

    @SuppressWarnings("resource")
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("audita_e2e_layer1")
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

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private static volatile boolean publicCompatibilityPatched = false;

    @Test
    void tenant_auth_login_refresh_logout_e2e() throws Exception {
        String slug = uniqueSlug("e2eauth");
        String email = "auth+" + slug + "@example.com";
        String password = "Password1!";

        setupTenantSchema(slug);
        insertTenant(slug);
        insertUser(slug, email, "Auth User", "ADMIN", "ACTIVE", password);

        HttpResponse<String> login = login(slug, email, password);
        assertThat(login.statusCode()).isEqualTo(200);

        String refreshCookie = extractRefreshCookie(login);
        assertThat(refreshCookie).isNotBlank();

        HttpResponse<String> refresh = postJson("/api/v1/auth/refresh", slug, "{}", null, refreshCookie);
        assertThat(refresh.statusCode()).isEqualTo(200);

        JsonNode refreshBody = objectMapper.readTree(refresh.body());
        assertThat(refreshBody.get("accessToken").asText()).isNotBlank();

        HttpResponse<String> logout = postJson("/api/v1/auth/logout", slug, "{}", null, refreshCookie);
        assertThat(logout.statusCode()).isEqualTo(204);
    }

    @Test
    void tenant_accept_invite_and_login_e2e() throws Exception {
        String slug = uniqueSlug("e2einvite");
        String email = "invite+" + slug + "@example.com";
        String password = "Password1!";

        setupTenantSchema(slug);
        insertTenant(slug);
        UUID userId = insertUser(slug, email, "Invite User", "REQUESTER", "PENDING", null);

        String rawToken = UUID.randomUUID().toString();
        insertInviteToken(slug, userId, rawToken);

        String acceptPayload = objectMapper.writeValueAsString(new AcceptInvitePayload(rawToken, "Accepted User", password));
        HttpResponse<String> accept = postJson("/api/v1/auth/accept-invite", slug, acceptPayload, null, null);
        assertThat(accept.statusCode()).isEqualTo(200);

        HttpResponse<String> login = login(slug, email, password);
        assertThat(login.statusCode()).isEqualTo(200);
    }

    @Test
    void tenant_change_request_lifecycle_e2e() throws Exception {
        String slug = uniqueSlug("e2ecr");
        String adminEmail = "admin+" + slug + "@example.com";
        String requesterEmail = "requester+" + slug + "@example.com";
        String approverEmail = "approver+" + slug + "@example.com";
        String password = "Password1!";

        setupTenantSchema(slug);
        insertTenant(slug);
        insertUser(slug, adminEmail, "Admin User", "ADMIN", "ACTIVE", password);
        insertUser(slug, requesterEmail, "Requester User", "REQUESTER", "ACTIVE", password);
        UUID approverId = insertUser(slug, approverEmail, "Approver User", "APPROVER", "ACTIVE", password);

        String adminToken = accessToken(login(slug, adminEmail, password));
        String requesterToken = accessToken(login(slug, requesterEmail, password));
        String approverToken = accessToken(login(slug, approverEmail, password));

        UUID requesterId = findUserIdByEmail(slug, requesterEmail);
        String crId = insertDraftChangeRequest(slug, requesterId).toString();

        String addApproverPayload = objectMapper.writeValueAsString(new AddApproverPayload(approverId.toString(), true));
        HttpResponse<String> addApprover = postJson("/api/v1/change-requests/" + crId + "/approvers", slug, addApproverPayload, adminToken, null);
        assertThat(addApprover.statusCode())
            .withFailMessage("Add approver failed: status=%s body=%s", addApprover.statusCode(), addApprover.body())
            .isEqualTo(201);

        HttpResponse<String> submit = postJson("/api/v1/change-requests/" + crId + "/submit", slug, "{}", requesterToken, null);
        assertThat(submit.statusCode())
            .withFailMessage("Submit CR failed: status=%s body=%s", submit.statusCode(), submit.body())
            .isEqualTo(200);
        assertThat(objectMapper.readTree(submit.body()).get("status").asText()).isEqualTo("PENDING_APPROVAL");

        HttpResponse<String> approve = postJson("/api/v1/change-requests/" + crId + "/approve", slug, "{}", approverToken, null);
        assertThat(approve.statusCode())
            .withFailMessage("Approve CR failed: status=%s body=%s", approve.statusCode(), approve.body())
            .isEqualTo(200);
        assertThat(objectMapper.readTree(approve.body()).get("status").asText()).isEqualTo("APPROVED");
    }

    @Test
    void tenant_notification_stream_token_resilience_e2e() throws Exception {
           String slug = uniqueSlug("e2esse");
        setupTenantSchema(slug);
        insertTenant(slug);

        String email = "stream." + slug + "@audita.test";
        String password = "P@ssw0rd!";
        insertUser(slug, email, "Stream User", "ADMIN", "ACTIVE", password);

        String accessToken = accessToken(login(slug, email, password));

        HttpResponse<String> issue = postJson("/api/v1/notifications/stream-token", slug, "{}", accessToken, null);
        assertThat(issue.statusCode()).isEqualTo(200);
        String streamToken = objectMapper.readTree(issue.body()).get("streamToken").asText();
        assertThat(streamToken).isNotBlank();

        HttpResponse<String> invalid = get("/api/v1/notifications/stream?streamToken=invalid-token", slug, null);
        assertThat(invalid.statusCode()).isEqualTo(403);

        HttpResponse<String> missing = get("/api/v1/notifications/stream", slug, null);
        assertThat(missing.statusCode()).isEqualTo(403);
    }

    private HttpResponse<String> login(String tenantSlug, String email, String password) throws IOException, InterruptedException {
        String payload = objectMapper.writeValueAsString(new LoginPayload(email, password));
        return postJson("/api/v1/auth/login", tenantSlug, payload, null, null);
    }

    private String accessToken(HttpResponse<String> loginResponse) throws Exception {
        assertThat(loginResponse.statusCode()).isEqualTo(200);
        return objectMapper.readTree(loginResponse.body()).get("accessToken").asText();
    }

    private String extractRefreshCookie(HttpResponse<String> response) {
        return response.headers().allValues("set-cookie").stream()
                .filter(v -> v.startsWith("refreshToken="))
                .map(v -> v.split(";", 2)[0])
                .findFirst()
                .orElse("");
    }

    private HttpResponse<String> postJson(String path,
                                          String tenantSlug,
                                          String body,
                                          String bearerToken,
                                          String cookie) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .header("X-Tenant-Slug", tenantSlug)
                .POST(HttpRequest.BodyPublishers.ofString(body));

        if (bearerToken != null) {
            builder.header("Authorization", "Bearer " + bearerToken);
        }
        if (cookie != null) {
            builder.header("Cookie", cookie);
        }

        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> get(String path,
                                     String tenantSlug,
                                     String bearer) throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
            .uri(URI.create("http://localhost:" + port + path))
                .header("X-Tenant-Slug", tenantSlug)
                .GET();
        if (bearer != null) {
            builder.header("Authorization", "Bearer " + bearer);
        }
        return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    private void setupTenantSchema(String slug) {
        patchPublicCompatibilityColumns();
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .schemas(slug)
                .locations("classpath:db/migration/tenant")
                .table("flyway_schema_history")
                .baselineOnMigrate(false)
                .validateOnMigrate(true)
                .load()
                .migrate();
        patchTenantCompatibilityColumns(slug);
        normalizeRoleNames(slug);
    }

    private void patchPublicCompatibilityColumns() {
        if (publicCompatibilityPatched) {
            return;
        }
        synchronized (CriticalFlowsE2EL1Test.class) {
            if (publicCompatibilityPatched) {
                return;
            }
            try (Connection c = connection()) {
                exec(c, "ALTER TABLE public.tenant_allowed_domains ADD COLUMN IF NOT EXISTS createdat TIMESTAMPTZ DEFAULT NOW()");
                exec(c, "ALTER TABLE public.super_admins ADD COLUMN IF NOT EXISTS createdat TIMESTAMPTZ DEFAULT NOW()");
                publicCompatibilityPatched = true;
            } catch (Exception e) {
                throw new IllegalStateException("Failed to patch public compatibility columns", e);
            }
        }
    }

    private void patchTenantCompatibilityColumns(String slug) {
        try (Connection c = connection()) {
            exec(c, "ALTER TABLE " + slug + ".users ADD COLUMN IF NOT EXISTS createdat TIMESTAMPTZ DEFAULT NOW()");
            exec(c, "ALTER TABLE " + slug + ".users ADD COLUMN IF NOT EXISTS updatedat TIMESTAMPTZ DEFAULT NOW()");
            exec(c, "ALTER TABLE " + slug + ".users ADD COLUMN IF NOT EXISTS \"createdAt\" TIMESTAMPTZ DEFAULT NOW()");
            exec(c, "ALTER TABLE " + slug + ".users ADD COLUMN IF NOT EXISTS \"updatedAt\" TIMESTAMPTZ DEFAULT NOW()");
            exec(c, "ALTER TABLE " + slug + ".users ADD COLUMN IF NOT EXISTS passwordhash VARCHAR(255)");
            exec(c, "ALTER TABLE " + slug + ".users ADD COLUMN IF NOT EXISTS \"passwordHash\" VARCHAR(255)");
            exec(c, "ALTER TABLE " + slug + ".users ADD COLUMN IF NOT EXISTS fullname VARCHAR(255)");
            exec(c, "ALTER TABLE " + slug + ".users ADD COLUMN IF NOT EXISTS \"fullName\" VARCHAR(255)");
            exec(c, "ALTER TABLE " + slug + ".users ADD COLUMN IF NOT EXISTS invitedby UUID");
            exec(c, "ALTER TABLE " + slug + ".users ADD COLUMN IF NOT EXISTS \"invitedBy\" UUID");

            exec(c, "ALTER TABLE " + slug + ".roles ADD COLUMN IF NOT EXISTS createdat TIMESTAMPTZ DEFAULT NOW()");
            exec(c, "ALTER TABLE " + slug + ".roles ADD COLUMN IF NOT EXISTS issystem BOOLEAN DEFAULT FALSE");

            exec(c, "ALTER TABLE " + slug + ".invite_tokens ADD COLUMN IF NOT EXISTS tokenhash VARCHAR(255)");
            exec(c, "ALTER TABLE " + slug + ".invite_tokens ADD COLUMN IF NOT EXISTS \"tokenHash\" VARCHAR(255)");
            exec(c, "ALTER TABLE " + slug + ".invite_tokens ADD COLUMN IF NOT EXISTS expiresat TIMESTAMPTZ");
            exec(c, "ALTER TABLE " + slug + ".invite_tokens ADD COLUMN IF NOT EXISTS \"expiresAt\" TIMESTAMPTZ");
            exec(c, "ALTER TABLE " + slug + ".invite_tokens ALTER COLUMN token_hash DROP NOT NULL");
            exec(c, "ALTER TABLE " + slug + ".invite_tokens ALTER COLUMN expires_at DROP NOT NULL");

            exec(c, "ALTER TABLE " + slug + ".refresh_tokens ADD COLUMN IF NOT EXISTS tokenhash VARCHAR(255)");
            exec(c, "ALTER TABLE " + slug + ".refresh_tokens ADD COLUMN IF NOT EXISTS \"tokenHash\" VARCHAR(255)");
            exec(c, "ALTER TABLE " + slug + ".refresh_tokens ADD COLUMN IF NOT EXISTS expiresat TIMESTAMPTZ");
            exec(c, "ALTER TABLE " + slug + ".refresh_tokens ADD COLUMN IF NOT EXISTS \"expiresAt\" TIMESTAMPTZ");
            exec(c, "ALTER TABLE " + slug + ".refresh_tokens ALTER COLUMN token_hash DROP NOT NULL");
            exec(c, "ALTER TABLE " + slug + ".refresh_tokens ALTER COLUMN expires_at DROP NOT NULL");

            exec(c, "ALTER TABLE " + slug + ".change_requests ADD COLUMN IF NOT EXISTS risklevel VARCHAR(20)");
            exec(c, "ALTER TABLE " + slug + ".change_requests ADD COLUMN IF NOT EXISTS \"riskLevel\" VARCHAR(20)");
            exec(c, "ALTER TABLE " + slug + ".change_requests ADD COLUMN IF NOT EXISTS approvallocked BOOLEAN DEFAULT FALSE");
            exec(c, "ALTER TABLE " + slug + ".change_requests ADD COLUMN IF NOT EXISTS \"approvalLocked\" BOOLEAN DEFAULT FALSE");
            exec(c, "ALTER TABLE " + slug + ".change_requests ADD COLUMN IF NOT EXISTS approvaltype VARCHAR(20)");
            exec(c, "ALTER TABLE " + slug + ".change_requests ADD COLUMN IF NOT EXISTS \"approvalType\" VARCHAR(20)");
            exec(c, "ALTER TABLE " + slug + ".change_requests ADD COLUMN IF NOT EXISTS scheduledstart TIMESTAMPTZ");
            exec(c, "ALTER TABLE " + slug + ".change_requests ADD COLUMN IF NOT EXISTS \"scheduledStart\" TIMESTAMPTZ");
            exec(c, "ALTER TABLE " + slug + ".change_requests ADD COLUMN IF NOT EXISTS scheduledend TIMESTAMPTZ");
            exec(c, "ALTER TABLE " + slug + ".change_requests ADD COLUMN IF NOT EXISTS \"scheduledEnd\" TIMESTAMPTZ");
            exec(c, "ALTER TABLE " + slug + ".change_requests ADD COLUMN IF NOT EXISTS affectedsystems TEXT[]");
            exec(c, "ALTER TABLE " + slug + ".change_requests ADD COLUMN IF NOT EXISTS \"affectedSystems\" TEXT[]");
            exec(c, "ALTER TABLE " + slug + ".change_requests ADD COLUMN IF NOT EXISTS sladeadline TIMESTAMPTZ");
            exec(c, "ALTER TABLE " + slug + ".change_requests ADD COLUMN IF NOT EXISTS \"slaDeadline\" TIMESTAMPTZ");
            exec(c, "ALTER TABLE " + slug + ".change_requests ADD COLUMN IF NOT EXISTS slabreached BOOLEAN DEFAULT FALSE");
            exec(c, "ALTER TABLE " + slug + ".change_requests ADD COLUMN IF NOT EXISTS \"slaBreached\" BOOLEAN DEFAULT FALSE");
            exec(c, "ALTER TABLE " + slug + ".change_requests ADD COLUMN IF NOT EXISTS createdat TIMESTAMPTZ DEFAULT NOW()");
            exec(c, "ALTER TABLE " + slug + ".change_requests ADD COLUMN IF NOT EXISTS \"createdAt\" TIMESTAMPTZ DEFAULT NOW()");
            exec(c, "ALTER TABLE " + slug + ".change_requests ADD COLUMN IF NOT EXISTS updatedat TIMESTAMPTZ DEFAULT NOW()");
            exec(c, "ALTER TABLE " + slug + ".change_requests ADD COLUMN IF NOT EXISTS \"updatedAt\" TIMESTAMPTZ DEFAULT NOW()");
            exec(c, "ALTER TABLE " + slug + ".change_requests ALTER COLUMN risk_level DROP NOT NULL");
            exec(c, "ALTER TABLE " + slug + ".change_requests ALTER COLUMN approval_type DROP NOT NULL");
            exec(c, "ALTER TABLE " + slug + ".change_requests ALTER COLUMN approval_locked DROP NOT NULL");
            exec(c, "ALTER TABLE " + slug + ".change_requests ALTER COLUMN sla_breached DROP NOT NULL");
            exec(c, "ALTER TABLE " + slug + ".change_requests ALTER COLUMN created_at DROP NOT NULL");
            exec(c, "ALTER TABLE " + slug + ".change_requests ALTER COLUMN updated_at DROP NOT NULL");

            exec(c, "ALTER TABLE " + slug + ".cr_approvers ADD COLUMN IF NOT EXISTS changerequestid UUID");
            exec(c, "ALTER TABLE " + slug + ".cr_approvers ADD COLUMN IF NOT EXISTS \"changeRequest\" UUID");
            exec(c, "ALTER TABLE " + slug + ".cr_approvers ADD COLUMN IF NOT EXISTS userid UUID");
            exec(c, "ALTER TABLE " + slug + ".cr_approvers ADD COLUMN IF NOT EXISTS isrequired BOOLEAN DEFAULT TRUE");
            exec(c, "ALTER TABLE " + slug + ".cr_approvers ADD COLUMN IF NOT EXISTS \"isRequired\" BOOLEAN DEFAULT TRUE");
            exec(c, "ALTER TABLE " + slug + ".cr_approvers ADD COLUMN IF NOT EXISTS rejectionreason TEXT");
            exec(c, "ALTER TABLE " + slug + ".cr_approvers ADD COLUMN IF NOT EXISTS \"rejectionReason\" TEXT");
            exec(c, "ALTER TABLE " + slug + ".cr_approvers ADD COLUMN IF NOT EXISTS decidedat TIMESTAMPTZ");
            exec(c, "ALTER TABLE " + slug + ".cr_approvers ADD COLUMN IF NOT EXISTS \"decidedAt\" TIMESTAMPTZ");
            exec(c, "ALTER TABLE " + slug + ".cr_approvers ADD COLUMN IF NOT EXISTS isadhoc BOOLEAN DEFAULT FALSE");
            exec(c, "ALTER TABLE " + slug + ".cr_approvers ADD COLUMN IF NOT EXISTS \"isAdHoc\" BOOLEAN DEFAULT FALSE");
            exec(c, "ALTER TABLE " + slug + ".cr_approvers ADD COLUMN IF NOT EXISTS createdat TIMESTAMPTZ DEFAULT NOW()");
            exec(c, "ALTER TABLE " + slug + ".cr_approvers ADD COLUMN IF NOT EXISTS \"createdAt\" TIMESTAMPTZ DEFAULT NOW()");
            exec(c, "ALTER TABLE " + slug + ".cr_approvers ALTER COLUMN is_required DROP NOT NULL");
            exec(c, "ALTER TABLE " + slug + ".cr_approvers ALTER COLUMN is_ad_hoc DROP NOT NULL");
            exec(c, "ALTER TABLE " + slug + ".cr_approvers ALTER COLUMN created_at DROP NOT NULL");

            exec(c, "ALTER TABLE " + slug + ".notifications ADD COLUMN IF NOT EXISTS recipientid UUID");
            exec(c, "ALTER TABLE " + slug + ".notifications ADD COLUMN IF NOT EXISTS isread BOOLEAN DEFAULT FALSE");
            exec(c, "ALTER TABLE " + slug + ".notifications ADD COLUMN IF NOT EXISTS createdat TIMESTAMPTZ DEFAULT NOW()");

            exec(c, "ALTER TABLE " + slug + ".activity_stream ADD COLUMN IF NOT EXISTS changerequestid UUID");
            exec(c, "ALTER TABLE " + slug + ".activity_stream ADD COLUMN IF NOT EXISTS \"changeRequest\" UUID");
            exec(c, "ALTER TABLE " + slug + ".activity_stream ADD COLUMN IF NOT EXISTS actorid UUID");
            exec(c, "ALTER TABLE " + slug + ".activity_stream ADD COLUMN IF NOT EXISTS \"actor\" UUID");
            exec(c, "ALTER TABLE " + slug + ".activity_stream ADD COLUMN IF NOT EXISTS actiontype VARCHAR(100)");
            exec(c, "ALTER TABLE " + slug + ".activity_stream ADD COLUMN IF NOT EXISTS \"actionType\" VARCHAR(100)");
            exec(c, "ALTER TABLE " + slug + ".activity_stream ADD COLUMN IF NOT EXISTS createdat TIMESTAMPTZ DEFAULT NOW()");
            exec(c, "ALTER TABLE " + slug + ".activity_stream ADD COLUMN IF NOT EXISTS \"createdAt\" TIMESTAMPTZ DEFAULT NOW()");
            exec(c, "ALTER TABLE " + slug + ".activity_stream ALTER COLUMN action_type DROP NOT NULL");
            exec(c, "ALTER TABLE " + slug + ".activity_stream ALTER COLUMN change_request_id DROP NOT NULL");
            exec(c, "ALTER TABLE " + slug + ".activity_stream ALTER COLUMN created_at DROP NOT NULL");
        } catch (Exception e) {
            throw new IllegalStateException("Failed to patch tenant compatibility columns", e);
        }
    }

    private void insertTenant(String slug) throws Exception {
        String sql = "INSERT INTO public.tenants (id, name, slug, status) VALUES (?, ?, ?, 'ACTIVE')";
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setString(2, "Tenant " + slug);
            ps.setString(3, slug);
            ps.executeUpdate();
        }
    }

    private void exec(Connection c, String sql) throws Exception {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.execute();
        }
    }

    private void normalizeRoleNames(String slug) {
        String sql = "UPDATE " + slug + ".roles SET name = UPPER(name)";
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to normalize role names", e);
        }
    }

    private UUID insertUser(String slug,
                            String email,
                            String fullName,
                            String roleName,
                            String status,
                            String rawPassword) throws Exception {
        try (Connection c = connection()) {
            UUID roleId = findRoleId(c, slug, roleName);
            UUID userId = UUID.randomUUID();

            String sql = "INSERT INTO " + slug + ".users (id, email, password_hash, passwordhash, \"passwordHash\", full_name, fullname, \"fullName\", role_id, status, created_at, createdat, \"createdAt\", updated_at, updatedat, \"updatedAt\") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW(), NOW(), NOW(), NOW(), NOW())";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setObject(1, userId);
                ps.setString(2, email);
                String hashed = rawPassword == null ? null : passwordEncoder.encode(rawPassword);
                ps.setString(3, hashed);
                ps.setString(4, hashed);
                ps.setString(5, hashed);
                ps.setString(6, fullName);
                ps.setString(7, fullName);
                ps.setString(8, fullName);
                ps.setObject(9, roleId);
                ps.setString(10, status);
                ps.executeUpdate();
            }
            return userId;
        }
    }

    private UUID findRoleId(Connection connection, String slug, String roleName) throws Exception {
        String sql = "SELECT id FROM " + slug + ".roles WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, roleName);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("Role not found: " + roleName);
                }
                return rs.getObject(1, UUID.class);
            }
        }
    }

    private UUID findUserIdByEmail(String slug, String email) throws Exception {
        String sql = "SELECT id FROM " + slug + ".users WHERE email = ?";
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (var rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new IllegalStateException("User not found: " + email);
                }
                return rs.getObject(1, UUID.class);
            }
        }
    }

    private UUID insertDraftChangeRequest(String slug, UUID createdById) throws Exception {
        UUID crId = UUID.randomUUID();
        String sql = """
                INSERT INTO %s.change_requests (
                    id, title, description, priority, risk_level, \"riskLevel\", category,
                    status, approval_type, \"approvalType\", approval_locked, \"approvalLocked\",
                    affected_systems, \"affectedSystems\", created_by,
                    created_at, updated_at, \"createdAt\", \"updatedAt\"
                ) VALUES (
                    ?, ?, ?, ?, ?, ?, ?,
                    ?, ?, ?, ?, ?,
                    ?::text[], ?::text[], ?,
                    NOW(), NOW(), NOW(), NOW()
                )
                """.formatted(slug);
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, crId);
            ps.setString(2, "E2E CR");
            ps.setString(3, "Critical flow");
            ps.setString(4, "HIGH");
            ps.setString(5, "MEDIUM");
            ps.setString(6, "MEDIUM");
            ps.setString(7, "Infra");
            ps.setString(8, "DRAFT");
            ps.setString(9, "LINEAR");
            ps.setString(10, "LINEAR");
            ps.setBoolean(11, false);
            ps.setBoolean(12, false);
            ps.setArray(13, c.createArrayOf("text", new String[]{"network"}));
            ps.setArray(14, c.createArrayOf("text", new String[]{"network"}));
            ps.setObject(15, createdById);
            ps.executeUpdate();
        }
        return crId;
    }

    private void insertInviteToken(String slug, UUID userId, String rawToken) throws Exception {
        String tokenHash = sha256Base64(rawToken);
        String sql = "INSERT INTO " + slug + ".invite_tokens (id, user_id, token_hash, tokenhash, \"tokenHash\", expires_at, expiresat, \"expiresAt\", used) VALUES (?, ?, ?, ?, ?, NOW() + interval '1 hour', NOW() + interval '1 hour', NOW() + interval '1 hour', false)";
        try (Connection c = connection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, UUID.randomUUID());
            ps.setObject(2, userId);
            ps.setString(3, tokenHash);
            ps.setString(4, tokenHash);
            ps.setString(5, tokenHash);
            ps.executeUpdate();
        }
    }

    private String sha256Base64(String input) {
        try {
            byte[] hash = java.security.MessageDigest.getInstance("SHA-256").digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash token", e);
        }
    }

    private String uniqueSlug(String prefix) {
        return prefix + "_" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private Connection connection() throws Exception {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    private record LoginPayload(String email, String password) {}

    private record AcceptInvitePayload(String token, String fullName, String password) {}

    private record AddApproverPayload(String userId, boolean isRequired) {}
}
