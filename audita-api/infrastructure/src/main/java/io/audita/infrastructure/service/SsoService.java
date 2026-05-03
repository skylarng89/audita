package io.audita.infrastructure.service;

import io.audita.application.port.SsoPort;
import io.audita.domain.exception.DomainException;
import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.model.OAuthProvider;
import io.audita.domain.model.UserStatus;
import io.audita.infrastructure.persistence.entity.*;
import io.audita.infrastructure.persistence.repository.*;import io.audita.infrastructure.security.AesEncryptionService;
import io.audita.infrastructure.security.JwtService;
import io.audita.infrastructure.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles OAuth2 SSO flows for Google OIDC and Microsoft Azure AD.
 *
 * Flow:
 *   1. {@link #buildAuthorizationUrl} → client redirects the browser to the provider
 *   2. Provider redirects back to the callback URL with code + state
 *   3. {@link #handleCallback} → exchanges code, JIT-provisions user if needed, issues tokens
 *
 * Per-tenant SSO credentials are stored AES-256 encrypted in {@code tenant_sso_configs}.
 * The state parameter ties the callback to the originating tenant slug, preventing
 * cross-tenant session fixation attacks.
 */
@Service
public class SsoService implements SsoPort {

    private static final Logger log = LoggerFactory.getLogger(SsoService.class);

    // State store: state-token → SsoState (expires after 10 minutes)
    private final ConcurrentHashMap<String, SsoState> pendingStates = new ConcurrentHashMap<>();
        // Frontend exchange store: one-time exchange code → SSO login result (expires after 90 seconds)
        private final ConcurrentHashMap<String, FrontendExchangeState> pendingExchanges = new ConcurrentHashMap<>();

    private final TenantSsoConfigRepository ssoConfigRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AesEncryptionService aesEncryptionService;
    private final JwtService jwtService;

    @Value("${audita.jwt.expiry-seconds:900}")
    private long jwtExpirySeconds;

    @Value("${audita.refresh-token.expiry-days:7}")
    private long refreshExpiryDays;

    @Value("${audita.sso.redirect-base-url}")
    private String redirectBaseUrl;

    public SsoService(TenantSsoConfigRepository ssoConfigRepository,
                      TenantRepository tenantRepository,
                      UserRepository userRepository,
                      OAuthAccountRepository oauthAccountRepository,
                      RefreshTokenRepository refreshTokenRepository,
                      AesEncryptionService aesEncryptionService,
                      JwtService jwtService) {
        this.ssoConfigRepository = ssoConfigRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.oauthAccountRepository = oauthAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.aesEncryptionService = aesEncryptionService;
        this.jwtService = jwtService;
    }

    // ── Step 1: Build authorization URL ─────────────────────────────────────────

        @Override
        public String buildAuthorizationUrl(String tenantSlug, OAuthProvider provider) {
                cleanupExpiredStates();
        TenantSsoConfigEntity config = loadSsoConfig(tenantSlug, provider);

        String state = generateState();
        pendingStates.put(state, new SsoState(tenantSlug, provider,
                System.currentTimeMillis() + 10 * 60 * 1000L));

        String callbackUrl = redirectBaseUrl + "/api/v1/auth/oauth/" + provider.name().toLowerCase() + "/callback";

        if (provider == OAuthProvider.GOOGLE) {
            return UriComponentsBuilder
                    .fromUriString("https://accounts.google.com/o/oauth2/v2/auth")
                    .queryParam("client_id", config.getClientId())
                    .queryParam("redirect_uri", callbackUrl)
                    .queryParam("response_type", "code")
                    .queryParam("scope", "openid email profile")
                    .queryParam("state", state)
                    .queryParam("access_type", "offline")
                    .build().toUriString();
        } else {
            // Microsoft — supports both common (multi-tenant) and single Azure AD tenant
            String tenantId = config.getMsTenantId() != null ? config.getMsTenantId() : "common";
            return UriComponentsBuilder
                    .fromUriString("https://login.microsoftonline.com/" + tenantId + "/oauth2/v2.0/authorize")
                    .queryParam("client_id", config.getClientId())
                    .queryParam("redirect_uri", callbackUrl)
                    .queryParam("response_type", "code")
                    .queryParam("scope", "openid email profile")
                    .queryParam("state", state)
                    .queryParam("response_mode", "query")
                    .build().toUriString();
        }
    }

    // ── Step 2: Handle callback, issue tokens ────────────────────────────────────

    @Transactional
        @Override
    public SsoResult handleCallback(OAuthProvider provider, String code, String state) {
                cleanupExpiredStates();
        SsoState ssoState = validateState(state);
        String tenantSlug = ssoState.tenantSlug();

        // Set tenant context so tenant-schema queries resolve correctly
        TenantContext.setCurrentTenant(tenantSlug);

        TenantSsoConfigEntity config = loadSsoConfig(tenantSlug, provider);
        String decryptedSecret = aesEncryptionService.decrypt(config.getClientSecret());
        String callbackUrl = redirectBaseUrl + "/api/v1/auth/oauth/" + provider.name().toLowerCase() + "/callback";

        OAuthUserInfo userInfo = exchangeCodeForUserInfo(provider, code, config.getClientId(),
                decryptedSecret, config.getMsTenantId(), callbackUrl);

        UserEntity user = resolveOrProvisionUser(provider, userInfo, tenantSlug);

        // Issue access token + rotating refresh token
        String role = user.getRole() != null ? user.getRole().getName() : "Requester";
        String accessToken = jwtService.issue(user.getId(), user.getEmail(), role, tenantSlug);

        String rawRefreshToken = generateSecureToken();
        RefreshTokenEntity refreshToken = new RefreshTokenEntity(
                user, AuthService.sha256(rawRefreshToken),
                OffsetDateTime.now().plusDays(refreshExpiryDays));
        refreshTokenRepository.save(refreshToken);

        log.info("SSO login via {} for user={} tenant={}", provider, user.getId(), tenantSlug);
        return new SsoResult(accessToken, rawRefreshToken, user.getId(), user.getEmail(),
                user.getFullName(), role, tenantSlug, jwtExpirySeconds);
    }

    @Override
    public String issueFrontendExchangeCode(SsoResult result) {
                cleanupExpiredStates();
                String code = generateSecureToken();
                long expiresAt = System.currentTimeMillis() + 90_000L;
                pendingExchanges.put(code, new FrontendExchangeState(result, expiresAt));
                return code;
    }

    @Override
    public SsoResult consumeFrontendExchangeCode(String code) {
                cleanupExpiredStates();
                FrontendExchangeState exchangeState = pendingExchanges.remove(code);
                if (exchangeState == null || System.currentTimeMillis() > exchangeState.expiresAt()) {
                        throw new DomainNotPermittedException(
                                        "INVALID_SSO_EXCHANGE", "SSO session has expired. Please sign in again.");
                }
                return exchangeState.result();
        }

    // ── OAuth2 code exchange ─────────────────────────────────────────────────────

    private OAuthUserInfo exchangeCodeForUserInfo(OAuthProvider provider, String code,
                                                   String clientId, String clientSecret,
                                                   String msTenantId, String redirectUri) {
        RestClient client = RestClient.create();

        // Exchange authorization code for tokens
        String tokenEndpoint = provider == OAuthProvider.GOOGLE
                ? "https://oauth2.googleapis.com/token"
                : "https://login.microsoftonline.com/" + (msTenantId != null ? msTenantId : "common") + "/oauth2/v2.0/token";

        @SuppressWarnings("unchecked")
        Map<String, Object> tokenResponse = client.post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=authorization_code"
                        + "&code=" + encode(code)
                        + "&client_id=" + encode(clientId)
                        + "&client_secret=" + encode(clientSecret)
                        + "&redirect_uri=" + encode(redirectUri))
                .retrieve()
                .body(Map.class);

        if (tokenResponse == null || !tokenResponse.containsKey("access_token")) {
            throw new DomainException("SSO token exchange failed.");
        }

        String accessToken = (String) tokenResponse.get("access_token");

        // Fetch user info
        String userinfoEndpoint = provider == OAuthProvider.GOOGLE
                ? "https://www.googleapis.com/oauth2/v3/userinfo"
                : "https://graph.microsoft.com/oidc/userinfo";

        @SuppressWarnings("unchecked")
        Map<String, Object> userInfo = client.get()
                .uri(userinfoEndpoint)
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .body(Map.class);

        if (userInfo == null) {
            throw new DomainException("Failed to retrieve user info from SSO provider.");
        }

        String sub = (String) userInfo.get("sub");
        String email = (String) userInfo.get("email");
        String name = (String) userInfo.getOrDefault("name",
                userInfo.getOrDefault("given_name", email));

        if (sub == null || email == null) {
            throw new DomainException("Incomplete user info received from SSO provider.");
        }

        return new OAuthUserInfo(sub, email.toLowerCase(), (String) name);
    }

    // ── JIT user provisioning (AUTH-013) + account linking (AUTH-014) ───────────

    private UserEntity resolveOrProvisionUser(OAuthProvider provider,
                                               OAuthUserInfo userInfo,
                                               String tenantSlug) {
        // 1. Look up by provider + sub (most stable identifier)
        Optional<OAuthAccountEntity> existing =
                oauthAccountRepository.findByProviderAndProviderSub(provider, userInfo.sub());
        if (existing.isPresent()) {
            UserEntity user = existing.get().getUser();
            if (user.getStatus() == UserStatus.SUSPENDED) {
                throw new DomainNotPermittedException("ACCOUNT_SUSPENDED",
                        "Your account has been suspended. Contact your administrator.");
            }
            return user;
        }

        // 2. Try to link by email match (AUTH-014)
        UserEntity user = userRepository.findByEmail(userInfo.email())
                .orElseGet(() -> {
                    // 3. JIT provision — new user with ACTIVE status; role assigned by admin later (AUTH-013)
                    tenantRepository.findBySlug(tenantSlug)
                            .orElseThrow(() -> new DomainNotPermittedException(
                                    "TENANT_NOT_FOUND", "Organisation not found."));
                    return userRepository.save(new UserEntity(userInfo.email(), userInfo.name()));
                });

        // Link the OAuth account so subsequent logins use provider+sub lookup
        OAuthAccountEntity oauthAccount = new OAuthAccountEntity(
                user, provider, userInfo.sub(), userInfo.email());
        oauthAccountRepository.save(oauthAccount);

        return user;
    }

    // ── State helpers ────────────────────────────────────────────────────────────

    private SsoState validateState(String state) {
        SsoState ssoState = pendingStates.remove(state);
        if (ssoState == null) {
            throw new DomainNotPermittedException("INVALID_STATE",
                    "Invalid or expired SSO state. Please start the sign-in process again.");
        }
        if (System.currentTimeMillis() > ssoState.expiresAt()) {
            throw new DomainNotPermittedException("INVALID_STATE",
                    "SSO session expired. Please start the sign-in process again.");
        }
        return ssoState;
    }

    private TenantSsoConfigEntity loadSsoConfig(String tenantSlug, OAuthProvider provider) {
        return ssoConfigRepository.findActiveByTenantSlugAndProvider(tenantSlug, provider)
                .orElseThrow(() -> new DomainNotPermittedException("SSO_NOT_CONFIGURED",
                        "SSO with " + provider.name() + " is not configured for this organisation."));
    }

    private static String generateState() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String generateSecureToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

        private void cleanupExpiredStates() {
                long now = System.currentTimeMillis();
                pendingStates.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
                pendingExchanges.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
        }

    // ── Records ──────────────────────────────────────────────────────────────────

    private record SsoState(String tenantSlug, OAuthProvider provider, long expiresAt) {}

        private record FrontendExchangeState(SsoResult result, long expiresAt) {}

    private record OAuthUserInfo(String sub, String email, String name) {}

}
