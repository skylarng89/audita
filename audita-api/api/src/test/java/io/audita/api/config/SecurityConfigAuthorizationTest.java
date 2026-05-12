package io.audita.api.config;

import io.audita.api.security.JwtAuthenticationFilter;
import io.audita.api.security.TenantResolutionFilter;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authorization.AuthorizationResult;
import org.springframework.security.web.access.intercept.RequestMatcherDelegatingAuthorizationManager;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SecurityConfigAuthorizationTest {

    @Test
    void unauthenticated_public_auth_endpoint_is_permitted() {
        AuthorizationResult result = authorizationManager().authorize(
                () -> null,
                request(HttpMethod.POST, "/api/v1/auth/session"));

        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isTrue();
    }

    @Test
    void unauthenticated_non_public_endpoint_is_rejected() {
        AuthorizationResult result = authorizationManager().authorize(
                () -> null,
                request(HttpMethod.GET, "/api/v1/protected"));

        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isFalse();
    }

    @Test
    void non_super_admin_cannot_access_platform_routes() {
        AuthorizationResult result = authorizationManager().authorize(
                () -> new TestingAuthenticationToken("user", "password", "ROLE_USER"),
                request(HttpMethod.GET, "/api/platform/v1/admin"));

        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isFalse();
    }

    @Test
    void super_admin_can_access_platform_routes() {
        AuthorizationResult result = authorizationManager().authorize(
                () -> new TestingAuthenticationToken("admin", "password", "ROLE_SUPER_ADMIN"),
                request(HttpMethod.GET, "/api/platform/v1/admin"));

        assertThat(result).isNotNull();
        assertThat(result.isGranted()).isTrue();
    }

    private RequestMatcherDelegatingAuthorizationManager authorizationManager() {
        SecurityConfig config = new SecurityConfig(
                mock(JwtAuthenticationFilter.class),
                mock(TenantResolutionFilter.class),
                "http://localhost:3000");
        return ReflectionTestUtils.invokeMethod(config, "apiAuthorizationManager");
    }

    private MockHttpServletRequest request(HttpMethod method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method.name(), path);
        request.setServletPath(path);
        request.setRequestURI(path);
        return request;
    }
}