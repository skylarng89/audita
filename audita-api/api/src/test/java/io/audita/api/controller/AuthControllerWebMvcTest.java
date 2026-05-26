package io.audita.api.controller;

import io.audita.application.port.AuthPort;
import io.audita.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import jakarta.servlet.http.Cookie;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerWebMvcTest {

        private MockMvc mockMvc;

        @Mock
        private AuthPort authPort;

        @BeforeEach
        void setUp() {
                AuthController controller = new AuthController(authPort);
                ReflectionTestUtils.setField(controller, "jwtExpirySeconds", 900L);
                ReflectionTestUtils.setField(controller, "refreshExpiryDays", 7L);
                ReflectionTestUtils.setField(controller, "refreshCookiePath", "/api/v1/auth");
                ReflectionTestUtils.setField(controller, "refreshCookieSecure", false);
                mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        }

        @org.junit.jupiter.api.AfterEach
        void tearDown() {
                TenantContext.clear();
        }

        @Test
        void login_scopes_refresh_cookie_to_auth_routes() throws Exception {
                when(authPort.loginSuperAdmin("owner@audita.io", "StrongPass1!A"))
                                .thenReturn(new AuthPort.LoginResult(
                                                "access-token",
                                                "refresh-token",
                                                UUID.randomUUID(),
                                                "owner@audita.io",
                                                "Owner",
                                                "SUPER_ADMIN",
                                                null));

                mockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  \"email\": \"owner@audita.io\",
                                                  \"password\": \"StrongPass1!A\"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(cookie().value("refreshToken", "refresh-token"))
                                .andExpect(cookie().path("refreshToken", "/api/v1/auth"))
                                .andExpect(cookie().secure("refreshToken", false));
        }

        @Test
        void logout_clears_refresh_cookie_on_auth_routes() throws Exception {
                mockMvc.perform(post("/api/v1/auth/logout"))
                                .andExpect(status().isNoContent())
                                .andExpect(cookie().maxAge("refreshToken", 0))
                                .andExpect(cookie().path("refreshToken", "/api/v1/auth"))
                                .andExpect(cookie().secure("refreshToken", false));

                verify(authPort).logout(isNull());
        }

        @Test
        void login_marks_refresh_cookie_secure_when_enabled() throws Exception {
                AuthController secureController = new AuthController(authPort);
                ReflectionTestUtils.setField(secureController, "jwtExpirySeconds", 900L);
                ReflectionTestUtils.setField(secureController, "refreshExpiryDays", 7L);
                ReflectionTestUtils.setField(secureController, "refreshCookiePath", "/api/v1/auth");
                ReflectionTestUtils.setField(secureController, "refreshCookieSecure", true);
                MockMvc secureMockMvc = MockMvcBuilders.standaloneSetup(secureController).build();

                when(authPort.loginSuperAdmin("owner@audita.io", "StrongPass1!A"))
                                .thenReturn(new AuthPort.LoginResult(
                                                "access-token",
                                                "refresh-token",
                                                UUID.randomUUID(),
                                                "owner@audita.io",
                                                "Owner",
                                                "SUPER_ADMIN",
                                                null));

                secureMockMvc.perform(post("/api/v1/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  \"email\": \"owner@audita.io\",
                                                  \"password\": \"StrongPass1!A\"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(cookie().secure("refreshToken", true));
        }

        @Test
        void session_returns_auth_response_without_rotating_refresh_cookie() throws Exception {
                UUID userId = UUID.randomUUID();
                when(authPort.restoreSession("refresh-token", "127.0.0.1", null))
                                .thenReturn(new AuthPort.LoginResult(
                                                "access-token",
                                                null,
                                                userId,
                                                "owner@audita.io",
                                                "Owner",
                                                "Admin",
                                                "acme"));

                mockMvc.perform(post("/api/v1/auth/session")
                                .header("X-Tenant-Slug", "acme")
                                .cookie(new Cookie("refreshToken", "refresh-token")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.accessToken").value("access-token"))
                                .andExpect(jsonPath("$.userId").value(userId.toString()))
                                .andExpect(cookie().doesNotExist("refreshToken"));
        }

        @Test
        void login_uses_resolved_tenant_context_over_header_value() throws Exception {
                UUID userId = UUID.randomUUID();
                TenantContext.setCurrentTenant("pixelpay-systems-limited");

                when(authPort.loginTenantUser(
                                eq("cogbuigwe@mypixelpay.com"),
                                eq("StrongPass1!A"),
                                eq("pixelpay-systems-limited"),
                                eq("127.0.0.1"),
                                nullable(String.class)))
                                .thenReturn(new AuthPort.LoginResult(
                                                "access-token",
                                                "refresh-token",
                                                userId,
                                                "cogbuigwe@mypixelpay.com",
                                                "Chinonso Ogbuigwe",
                                                "Requester",
                                                "pixelpay-systems-limited"));

                mockMvc.perform(post("/api/v1/auth/login")
                                .header("X-Tenant-Slug", "cm")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                                {
                                                  \"email\": \"cogbuigwe@mypixelpay.com\",
                                                  \"password\": \"StrongPass1!A\"
                                                }
                                                """))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.tenantSlug").value("pixelpay-systems-limited"));

                verify(authPort).loginTenantUser(
                                eq("cogbuigwe@mypixelpay.com"),
                                eq("StrongPass1!A"),
                                eq("pixelpay-systems-limited"),
                                eq("127.0.0.1"),
                                nullable(String.class));
        }
}
