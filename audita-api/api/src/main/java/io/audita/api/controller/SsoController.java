package io.audita.api.controller;

import io.audita.api.dto.response.AuthResponse;
import io.audita.domain.model.OAuthProvider;
import io.audita.infrastructure.service.SsoService;
import io.audita.infrastructure.service.SsoService.SsoResult;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Handles SSO initiation and OAuth2 callbacks for Google and Microsoft.
 *
 * Endpoints are intentionally GET (browser navigations) so the browser
 * can follow redirects. Both are marked permitAll in SecurityConfig.
 *
 * Callback flow:
 *   1. User clicks SSO button → browser navigates to /api/v1/auth/oauth/{provider}?tenant=acme
 *   2. Server generates state, redirects browser to provider
 *   3. Provider redirects to /api/v1/auth/oauth/{provider}/callback?code=...&state=...
 *   4. Server validates state, exchanges code, issues tokens, redirects to frontend
 */
@RestController
@RequestMapping("/api/v1/auth/oauth")
public class SsoController {

    private static final String REFRESH_COOKIE = "refreshToken";

    private final SsoService ssoService;

    @Value("${audita.sso.frontend-base-url}")
    private String frontendBaseUrl;

    @Value("${audita.refresh-token.expiry-days:7}")
    private long refreshExpiryDays;

    public SsoController(SsoService ssoService) {
        this.ssoService = ssoService;
    }

    /** Initiates Google SSO — redirects browser to Google's authorization endpoint. */
    @GetMapping("/google")
    public void initiateGoogle(
            @RequestParam("tenant") String tenantSlug,
            HttpServletResponse response) throws IOException {
        redirect(response, ssoService.buildAuthorizationUrl(tenantSlug, OAuthProvider.GOOGLE));
    }

    /** Initiates Microsoft SSO — redirects browser to Azure AD's authorization endpoint. */
    @GetMapping("/microsoft")
    public void initiateMicrosoft(
            @RequestParam("tenant") String tenantSlug,
            HttpServletResponse response) throws IOException {
        redirect(response, ssoService.buildAuthorizationUrl(tenantSlug, OAuthProvider.MICROSOFT));
    }

    /** Google OAuth2 callback. */
    @GetMapping("/google/callback")
    public void googleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response) throws IOException {
        handleCallback(OAuthProvider.GOOGLE, code, state, response);
    }

    /** Microsoft OAuth2 callback. */
    @GetMapping("/microsoft/callback")
    public void microsoftCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response) throws IOException {
        handleCallback(OAuthProvider.MICROSOFT, code, state, response);
    }

    // ── Shared callback handler ──────────────────────────────────────────────────

    private void handleCallback(OAuthProvider provider, String code, String state,
                                 HttpServletResponse response) throws IOException {
        SsoResult result;
        try {
            result = ssoService.handleCallback(provider, code, state);
        } catch (Exception _) {
            // Redirect to sign-in with error code so the frontend can show a message
            redirect(response, frontendBaseUrl + "/auth/sign-in?sso_error=CALLBACK_FAILED");
            return;
        }

        // Set refresh token as HttpOnly cookie — access token goes to frontend via query param
        // (refresh token remains the durable credential; access token is exchanged server-side)
        setRefreshCookie(response, result.rawRefreshToken());

        String exchangeCode = ssoService.issueFrontendExchangeCode(result);

        // Redirect to frontend with a short-lived one-time exchange code (never with access tokens).
        String redirectUrl = frontendBaseUrl + "/auth/sso-callback"
            + "?code=" + exchangeCode;

        redirect(response, redirectUrl);
    }

        @PostMapping("/exchange")
        public ResponseEntity<AuthResponse> exchangeCode(@RequestParam("code") String code) {
        SsoResult result = ssoService.consumeFrontendExchangeCode(code);
        AuthResponse response = AuthResponse.of(
            result.accessToken(),
            result.expiresIn(),
            result.userId(),
            result.email(),
            result.fullName(),
            result.role(),
            result.tenantSlug());
        return ResponseEntity.ok(response);
        }

    private void setRefreshCookie(HttpServletResponse response, String rawToken) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, rawToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/api/v1/auth/refresh");
        cookie.setAttribute("SameSite", "Lax"); // Lax required for cross-origin redirect callbacks
        cookie.setMaxAge((int) (refreshExpiryDays * 24 * 60 * 60));
        response.addCookie(cookie);
    }

    private void redirect(HttpServletResponse response, String url) throws IOException {
        response.sendRedirect(url);
    }
}
