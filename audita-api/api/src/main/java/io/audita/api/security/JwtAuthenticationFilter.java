package io.audita.api.security;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.audita.infrastructure.persistence.entity.UserEntity;
import io.audita.infrastructure.persistence.repository.UserRepository;
import io.audita.infrastructure.security.JwtService;
import io.audita.infrastructure.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Validates the Bearer token on every request.
 * On success: populates SecurityContext + TenantContext.
 * On failure: continues the filter chain unauthenticated (Spring Security handles 401).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final DataSource dataSource;

    // Cache active tenant slugs (TTL 60s) to avoid DB queries on every request
    private final com.github.benmanes.caffeine.cache.Cache<String, Boolean> activeTenants =
            Caffeine.newBuilder()
                    .expireAfterWrite(60, TimeUnit.SECONDS)
                    .maximumSize(10_000)
                    .build();

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository userRepository, DataSource dataSource) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
        this.dataSource = dataSource;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        boolean bootstrapRequest = isBootstrapRequest(request);
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            logBootstrapWithoutToken(bootstrapRequest, request);
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (!jwtService.isValid(token)) {
            logBootstrapInvalidToken(bootstrapRequest, request);
            chain.doFilter(request, response);
            return;
        }

        Claims claims = jwtService.parse(token);
        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);
        String role = claims.get("role", String.class);
        List<String> roles = extractStringListClaim(claims, "roles");
        List<String> permissions = extractStringListClaim(claims, "permissions");
        String tenantSlug = claims.get("tenantSlug", String.class);
        if (tenantSlug != null && !tenantSlug.isBlank()) {
            tenantSlug = tenantSlug.toLowerCase(Locale.ROOT);
            if (!isTenantActive(tenantSlug)) {
                log.warn("Rejected request: tenant {} is not active", tenantSlug);
                chain.doFilter(request, response);
                return;
            }
        }

        logBootstrapAuthenticated(bootstrapRequest, request, role, tenantSlug);

        UserPrincipal principal = resolvePrincipal(userId, email, role, roles, permissions, tenantSlug);

        // Token version validation for tenant users (not Super Admin)
        // Must run AFTER TenantContext is set by resolvePrincipal
        if (!"SUPER_ADMIN".equals(role)) {
            Integer tokenTv = claims.get("tv", Integer.class);
            int expectedTv = tokenTv != null ? tokenTv : 0;
            Optional<UserEntity> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty() || userOpt.get().getTokenVersion() != expectedTv) {
                chain.doFilter(request, response);
                return;
            }
        }

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
        // TenantContext is cleared by TenantResolutionFilter in its finally block
    }

    private boolean isBootstrapRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/platform/v1/bootstrap") || uri.startsWith("/api/platform/v1/setup");
    }

    private UserPrincipal resolvePrincipal(
            UUID userId,
            String email,
            String role,
            List<String> roles,
            List<String> permissions,
            String tenantSlug) {
        if ("SUPER_ADMIN".equals(role)) {
            return UserPrincipal.ofSuperAdmin(userId, email);
        }

        String requestTenant = TenantContext.getCurrentTenant();
        if (tenantSlug == null || tenantSlug.isBlank()) {
            throw new AccessDeniedException("Invalid tenant token context.");
        }
        if (requestTenant != null && !requestTenant.equals(tenantSlug)) {
            throw new AccessDeniedException("Tenant context mismatch.");
        }

        TenantContext.setCurrentTenant(tenantSlug);
        return UserPrincipal.ofTenantUser(userId, email, role, roles, permissions, tenantSlug);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractStringListClaim(Claims claims, String claimName) {
        Object raw = claims.get(claimName);
        if (raw instanceof List<?> values) {
            return values.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .toList();
        }
        return Collections.emptyList();
    }

    private boolean isTenantActive(String slug) {
        Boolean cached = activeTenants.getIfPresent(slug);
        if (cached != null) {
            return cached;
        }
        boolean active = queryTenantActive(slug);
        activeTenants.put(slug, active);
        return active;
    }

    private boolean queryTenantActive(String slug) {
        String sql = "SELECT status FROM public.tenants WHERE slug = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, slug);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String status = resultSet.getString(1);
                    return "ACTIVE".equals(status);
                }
                return false;
            }
        } catch (SQLException ex) {
            log.error("Tenant active status query failed for slug={}", slug, ex);
            return false;
        }
    }

    private void logBootstrapWithoutToken(boolean bootstrapRequest, HttpServletRequest request) {
        if (bootstrapRequest && log.isInfoEnabled()) {
            String method = request.getMethod();
            String path = request.getRequestURI();
            String origin = request.getHeader("Origin");
            String referer = request.getHeader("Referer");
            boolean cookiesPresent = request.getHeader("Cookie") != null;
            log.info("Bootstrap request has no bearer token: method={} path={} origin={} referer={} cookiesPresent={}",
                    method, path, origin, referer, cookiesPresent);
        }
    }

    private void logBootstrapInvalidToken(boolean bootstrapRequest, HttpServletRequest request) {
        if (bootstrapRequest && log.isWarnEnabled()) {
            String method = request.getMethod();
            String path = request.getRequestURI();
            String userAgent = request.getHeader("User-Agent");
            log.warn("Bootstrap request provided invalid bearer token: method={} path={} userAgent={}",
                    method, path, userAgent);
        }
    }

    private void logBootstrapAuthenticated(boolean bootstrapRequest,
            HttpServletRequest request,
            String role,
            String tenantSlug) {
        if (bootstrapRequest && log.isWarnEnabled()) {
            String method = request.getMethod();
            String path = request.getRequestURI();
            log.warn("Bootstrap request includes authenticated JWT context: method={} path={} role={} tenantSlug={}",
                    method, path, role, tenantSlug);
        }
    }
}
