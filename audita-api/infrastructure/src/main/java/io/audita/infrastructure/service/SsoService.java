package io.audita.infrastructure.service;

import io.audita.application.port.SsoPort;
import io.audita.domain.exception.DomainException;
import io.audita.domain.exception.DomainNotPermittedException;
import io.audita.domain.model.OAuthProvider;
import io.audita.domain.model.UserStatus;
import io.audita.infrastructure.persistence.entity.*;
import io.audita.infrastructure.persistence.repository.*;
import io.audita.infrastructure.security.AesEncryptionService;
import io.audita.infrastructure.security.JwtService;
import io.audita.infrastructure.security.RoleHierarchy;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles OAuth2 SSO flows for Google OIDC and Microsoft Azure AD.
 *
 * Flow:
 * 1. {@link #buildAuthorizationUrl} → client redirects the browser to the provider
 * 2. Provider redirects back to the callback URL with code + state
 * 3. {@link #handleCallback} → exchanges code, JIT-provisions user if needed, issues tokens
 *
 * Per-tenant SSO credentials are stored AES-256 encrypted in {@code tenant_sso_configs}.
 * The state parameter ties the callback to the originating tenant slug, preventing
 * cross-tenant session fixation attacks.
 *
 * SSO state is stored via {@link SsoStateStore}. The default {@link InMemorySsoStateStore}
 * implementation is suitable for single-instance deployments only. For multi-instance HA,
 * replace with a database-backed or Redis-backed implementation.
 */
@Service
public class SsoService implements SsoPort {

    private static final Logger log = LoggerFactory.getLogger(SsoService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SsoStateStore stateStore;

    private final TenantSsoConfigRepository ssoConfigRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final OAuthAccountRepository oauthAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TenantAllowedDomainRepository allowedDomainRepository;
    private final AesEncryptionService aesEncryptionService;
    private final JwtService jwtService;

    @Value("${audita.jwt.expiry-seconds:900}")
    private long jwtExpirySeconds;

    @Value("${audita.refresh-token.expiry-days:7}")
    private long refreshExpiryDays;

    @Value("${audita.sso.redirect-base-url}")
    private String redirectBaseUrl;

    public SsoService(SsoStateStore stateStore,
            TenantSsoConfigRepository ssoConfigRepository,
            TenantRepository tenantRepository,
            UserRepository userRepository,
            OAuthAccountRepository oauthAccountRepository,
            RefreshTokenRepository refreshTokenRepository,
            TenantAllowedDomainRepository allowedDomainRepository,
            AesEncryptionService aesEncryptionService,
            JwtService jwtService) {
        this.stateStore = stateStore;
        this.ssoConfigRepository = ssoConfigRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.oauthAccountRepository = oauthAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.allowedDomainRepository = allowedDomainRepository;
        this.aesEncryptionService = aesEncryptionService;
        this.jwtService = jwtService;
    }

    // ── Step 1: Build authorization URL ─────────────────────────────────────────

    @Override
    public String buildAuthorizationUrl(String tenantSlug, OAuthProvider provider) {
        cleanupExpiredStates();
        TenantSsoConfigEntity config = loadSsoConfig(tenantSlug, provider);

        String state = generateState();
        stateStore.putState(state, tenantSlug, provider,
                System.currentTimeMillis() + 10 * 60 * 1000L);

        String callbackUrl = redirectBaseUrl + "/api/v1/auth/oauth/" + provider.name().toLowerCase()
                + "/callback";

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
        SsoState ssoState = stateStore.validateAndRemoveState(state);
        String tenantSlug = ssoState.tenantSlug();

        // Set tenant context so tenant-schema queries resolve correctly.
        // Wrapped in try/finally to prevent tenant context leak on error paths.
        TenantContext.setCurrentTenant(tenantSlug);
        try {
            TenantSsoConfigEntity config = loadSsoConfig(tenantSlug, provider);
            String decryptedSecret = aesEncryptionService.decrypt(config.getClientSecret());
            String callbackUrl = redirectBaseUrl + "/api/v1/auth/oauth/" + provider.name().toLowerCase()
                    + "/callback";

            OAuthUserInfo userInfo = exchangeCodeForUserInfo(provider, code, config.getClientId(),
                    decryptedSecret, config.getMsTenantId(), callbackUrl);

            UserEntity user = resolveOrProvisionUser(provider, userInfo, tenantSlug);

            List<RoleEntity> assignedRoles = resolveAssignedRoles(user);
            String role = RoleHierarchy.highestRoleNameOrDefault(assignedRoles, "Requester");
            List<String> roleNames = assignedRoles.stream().map(RoleEntity::getName).toList();
            List<String> permissionCodes = assignedRoles.stream()
                    .flatMap(r -> r.getPermissions().stream())
                    .map(permission -> permission.getCode() == null ? "" : permission.getCode())
                    .filter(permissionCode -> !permissionCode.isBlank())
                    .map(String::toLowerCase)
                    .distinct()
                    .toList();
            String accessToken = jwtService.issue(user.getId(), user.getEmail(), role, roleNames, permissionCodes,
                    tenantSlug, user.getTokenVersion());

            String rawRefreshToken = generateSecureToken();
            RefreshTokenEntity refreshToken = new RefreshTokenEntity(
                    user, AuthService.sha256(rawRefreshToken),
                    OffsetDateTime.now().plusDays(refreshExpiryDays));
            refreshTokenRepository.save(refreshToken);

            log.info("SSO login via {} for user={} tenant={}", provider, user.getId(), tenantSlug);
            return new SsoResult(accessToken, rawRefreshToken, user.getId(), user.getEmail(),
                    user.getFullName(), role, tenantSlug, jwtExpirySeconds);
        } finally {
            TenantContext.clear();
        }
    }

    private List<RoleEntity> resolveAssignedRoles(UserEntity user) {
        LinkedHashSet<RoleEntity> roles = new LinkedHashSet<>(user.getRoles());
        if (roles.isEmpty() && user.getRole() != null) {
            roles.add(user.getRole());
        }
        return List.copyOf(roles);
    }

    @Override
    public String issueFrontendExchangeCode(SsoResult result) {
        cleanupExpiredStates();
        String code = generateSecureToken();
        long expiresAt = System.currentTimeMillis() + 90_000L;
        stateStore.putExchange(code, result, expiresAt);
        return code;
    }

    @Override
    public SsoResult consumeFrontendExchangeCode(String code) {
        cleanupExpiredStates();
        FrontendExchangeState exchangeState = stateStore.consumeExchange(code);
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

        String tokenEndpoint;
        if (provider == OAuthProvider.GOOGLE) {
            tokenEndpoint = "https://oauth2.googleapis.com/token";
        } else {
            String microsoftTenantId = msTenantId != null ? msTenantId : "common";
            tokenEndpoint = "https://login.microsoftonline.com/" + microsoftTenantId + "/oauth2/v2.0/token";
        }

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

        return new OAuthUserInfo(sub, email.toLowerCase(), name);
    }

    // ── JIT user provisioning (AUTH-013) + account linking (AUTH-014) ───────────

    private UserEntity resolveOrProvisionUser(OAuthProvider provider,
            OAuthUserInfo userInfo,
            String tenantSlug) {
        Optional<OAuthAccountEntity> existing = oauthAccountRepository.findByProviderAndProviderSub(provider,
                userInfo.sub());
        if (existing.isPresent()) {
            UserEntity user = existing.get().getUser();
            if (user.getStatus() == UserStatus.SUSPENDED) {
                throw new DomainNotPermittedException("ACCOUNT_SUSPENDED",
                        "Your account has been suspended. Contact your administrator.");
            }
            return user;
        }

        UserEntity user = userRepository.findByEmail(userInfo.email())
                .orElseGet(() -> {
                    tenantRepository.findBySlug(tenantSlug)
                            .orElseThrow(() -> new DomainNotPermittedException(
                                    "TENANT_NOT_FOUND", "Organisation not found."));
                    checkDomainWhitelist(userInfo.email(), tenantSlug);
                    return userRepository.save(new UserEntity(userInfo.email(), userInfo.name()));
                });

        OAuthAccountEntity oauthAccount = new OAuthAccountEntity(
                user, provider, userInfo.sub(), userInfo.email());
        oauthAccountRepository.save(oauthAccount);

        return user;
    }

    // ── State helpers ────────────────────────────────────────────────────────────

    private TenantSsoConfigEntity loadSsoConfig(String tenantSlug, OAuthProvider provider) {
        return ssoConfigRepository.findActiveByTenantSlugAndProvider(tenantSlug, provider)
                .orElseThrow(() -> new DomainNotPermittedException("SSO_NOT_CONFIGURED",
                        "SSO with " + provider.name() + " is not configured for this organisation."));
    }

    private static String generateState() {
        return generateToken();
    }

    private static String generateSecureToken() {
        return generateToken();
    }

    private static String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void checkDomainWhitelist(String email, String tenantSlug) {
        List<TenantAllowedDomainEntity> allowed = allowedDomainRepository.findByTenantSlug(tenantSlug);
        if (allowed.isEmpty()) {
            return;
        }
        int atIndex = email.indexOf('@');
        String emailDomain = (atIndex >= 0 ? email.substring(atIndex + 1) : email).toLowerCase();
        boolean permitted = allowed.stream()
                .anyMatch(d -> d.getDomain().equalsIgnoreCase(emailDomain));
        if (!permitted) {
            log.warn("SSO JIT provisioning rejected: domain not whitelisted email={} tenant={}",
                    email, tenantSlug);
            throw new DomainNotPermittedException("DOMAIN_NOT_PERMITTED",
                    "Your email domain is not authorised for this organisation.");
        }
    }

    private static String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private void cleanupExpiredStates() {
        stateStore.cleanupExpired();
    }

    // ── Records ──────────────────────────────────────────────────────────────────

    record SsoState(String tenantSlug, OAuthProvider provider, long expiresAt) {
    }

    record FrontendExchangeState(SsoResult result, long expiresAt) {
    }

    private record OAuthUserInfo(String sub, String email, String name) {
    }
}
