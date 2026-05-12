package io.audita.api.controller;

import io.audita.application.port.AuthPort;
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
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
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
                .andExpect(cookie().path("refreshToken", "/api/v1/auth"));
    }

    @Test
    void logout_clears_refresh_cookie_on_auth_routes() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("refreshToken", 0))
                .andExpect(cookie().path("refreshToken", "/api/v1/auth"));

        verify(authPort).logout(isNull());
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
                .cookie(new Cookie("refreshToken", "refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(cookie().doesNotExist("refreshToken"));
    }
}