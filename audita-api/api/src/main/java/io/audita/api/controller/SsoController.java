package io.audita.api.controller;

import io.audita.api.dto.request.ExchangeSsoCodeRequest;
import io.audita.api.dto.response.AuthResponse;
import io.audita.application.port.SsoPort;
import io.audita.application.port.SsoPort.SsoResult;
import io.audita.domain.model.OAuthProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/v1/auth/oauth")
public class SsoController {

    private static final String REFRESH_COOKIE = "refreshToken";

    private final SsoPort ssoService;

    @Value("${audita.sso.frontend-base-url}")
    private String frontendBaseUrl;

    @Value("${audita.refresh-token.expiry-days:7}")
    private long refreshExpiryDays;

    @Value("${audita.auth.cookie-path:/api/v1/auth}")
    private String refreshCookiePath;

    public SsoController(SsoPort ssoService) {
        this.ssoService = ssoService;
    }

    @GetMapping("/google")
    public void initiateGoogle(
            @RequestParam("tenant") String tenantSlug,
            HttpServletResponse response) throws IOException {
        redirect(response, ssoService.buildAuthorizationUrl(tenantSlug, OAuthProvider.GOOGLE));
    }

    @GetMapping("/microsoft")
    public void initiateMicrosoft(
            @RequestParam("tenant") String tenantSlug,
            HttpServletResponse response) throws IOException {
        redirect(response, ssoService.buildAuthorizationUrl(tenantSlug, OAuthProvider.MICROSOFT));
    }

    @GetMapping("/google/callback")
    public void googleCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response) throws IOException {
        handleCallback(OAuthProvider.GOOGLE, code, state, response);
    }

    @GetMapping("/microsoft/callback")
    public void microsoftCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletResponse response) throws IOException {
        handleCallback(OAuthProvider.MICROSOFT, code, state, response);
    }

    private void handleCallback(OAuthProvider provider, String code, String state,
            HttpServletResponse response) throws IOException {
        SsoResult result;
        try {
            result = ssoService.handleCallback(provider, code, state);
        } catch (Exception _) {
            redirect(response, frontendBaseUrl + "/auth/sign-in?sso_error=CALLBACK_FAILED");
            return;
        }

        setRefreshCookie(response, result.rawRefreshToken());

        String exchangeCode = ssoService.issueFrontendExchangeCode(result);

        String redirectUrl = frontendBaseUrl + "/auth/sso-callback"
                + "#code=" + URLEncoder.encode(exchangeCode, StandardCharsets.UTF_8);

        redirect(response, redirectUrl);
    }

    @PostMapping("/exchange")
    public ResponseEntity<AuthResponse> exchangeCode(@Valid @RequestBody ExchangeSsoCodeRequest request) {
        SsoResult result = ssoService.consumeFrontendExchangeCode(request.code());
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
        cookie.setPath(refreshCookiePath);
        cookie.setAttribute("SameSite", "Lax");
        cookie.setMaxAge((int) (refreshExpiryDays * 24 * 60 * 60));
        response.addCookie(cookie);
    }

    private void redirect(HttpServletResponse response, String url) throws IOException {
        response.sendRedirect(url);
    }
}
