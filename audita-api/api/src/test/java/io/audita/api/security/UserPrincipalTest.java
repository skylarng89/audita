package io.audita.api.security;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    void tenant_user_username_returns_user_id_string() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.ofTenantUser(userId, "user@example.com", "ADMIN", "tenant_a");

        assertThat(principal.getUsername()).isEqualTo(userId.toString());
    }

    @Test
    void super_admin_username_returns_user_id_string() {
        UUID userId = UUID.randomUUID();
        UserPrincipal principal = UserPrincipal.ofSuperAdmin(userId, "super@example.com");

        assertThat(principal.getUsername()).isEqualTo(userId.toString());
    }
}
