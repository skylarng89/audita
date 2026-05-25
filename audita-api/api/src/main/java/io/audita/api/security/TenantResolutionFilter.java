package io.audita.api.security;

import io.audita.infrastructure.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Resolves the active tenant and stores it in {@link TenantContext}.
 *
 * Resolution order (first match wins):
 * <ol>
 *   <li>{@code X-Forwarded-Host} subdomain → stored subdomain mapping</li>
 *   <li>{@code X-Tenant-Slug} header → direct slug lookup</li>
 * </ol>
 *
 * Subdomain resolution has priority because it is server-side (Nginx sets
 * X-Forwarded-Host) and cannot be spoofed by the client. The slug header
 * is a trusted fallback for direct API access or internal service calls.
 *
 * Must run BEFORE {@code JwtAuthenticationFilter} so that unauthenticated
 * endpoints (login, refresh, SSO callbacks) can also resolve the correct
 * tenant schema for their DB queries.
 */
@Component
public class TenantResolutionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantResolutionFilter.class);

    private static final String TENANT_HEADER = "X-Tenant-Slug";
    private static final String FORWARDED_HOST_HEADER = "X-Forwarded-Host";
    private static final Pattern TENANT_SLUG_PATTERN = Pattern.compile("^[a-z0-9-]{1,100}$");
    private static final Set<String> RESERVED_SUBDOMAINS = Set.of("www", "app", "api", "mail", "smtp");

    private final DataSource dataSource;

    public TenantResolutionFilter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        boolean bootstrapRequest = isBootstrapRequest(request);
        String resolvedSlug = null;

        // 1. Try subdomain resolution from X-Forwarded-Host (server-side, trusted)
        String subdomain = extractSubdomain(request);
        if (subdomain != null) {
            String slugBySubdomain = findSlugBySubdomain(subdomain);
            if (slugBySubdomain != null) {
                resolvedSlug = slugBySubdomain;
                if (log.isDebugEnabled()) {
                    log.debug("Resolved tenant via subdomain: subdomain={} slug={}", subdomain, resolvedSlug);
                }
            }
        }

        // 2. Fall back to X-Tenant-Slug header (client-supplied, trusted for internal callers)
        if (resolvedSlug == null) {
            String headerSlug = request.getHeader(TENANT_HEADER);
            if (headerSlug != null && !headerSlug.isBlank()) {
                String normalizedSlug = headerSlug.trim().toLowerCase(Locale.ROOT);
                if (!TENANT_SLUG_PATTERN.matcher(normalizedSlug).matches()) {
                    if (log.isWarnEnabled()) {
                        log.warn("Rejected request due to invalid tenant slug: method={} path={} rawTenantSlug={}",
                                request.getMethod(), request.getRequestURI(), headerSlug);
                    }
                    throw new AccessDeniedException("Invalid tenant slug.");
                }
                if (bootstrapRequest) {
                    log.warn("Rejected bootstrap request containing tenant header: method={} path={} tenantSlug={}",
                            request.getMethod(), request.getRequestURI(), normalizedSlug);
                    throw new AccessDeniedException("Tenant header is not allowed for bootstrap requests.");
                }
                if (!tenantExists(normalizedSlug)) {
                    log.warn("Rejected request due to unknown tenant slug: method={} path={} tenantSlug={}",
                            request.getMethod(), request.getRequestURI(), normalizedSlug);
                    throw new AccessDeniedException("Unknown tenant.");
                }
                resolvedSlug = normalizedSlug;
            }
        }

        if (bootstrapRequest && resolvedSlug != null) {
            log.warn("Rejected bootstrap request containing tenant context: method={} path={} slug={}",
                    request.getMethod(), request.getRequestURI(), resolvedSlug);
            throw new AccessDeniedException("Tenant context is not allowed for bootstrap requests.");
        }

        if (resolvedSlug != null) {
            TenantContext.setCurrentTenant(resolvedSlug);
        } else if (bootstrapRequest) {
            log.info("Bootstrap request has no tenant context: method={} path={}",
                    request.getMethod(), request.getRequestURI());
        }

        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Extracts the subdomain from the X-Forwarded-Host header.
     * For "cm.mypixelpay.com" returns "cm".
     * Returns null if the host has fewer than 3 parts or the subdomain is reserved.
     */
    String extractSubdomain(HttpServletRequest request) {
        String forwardedHost = request.getHeader(FORWARDED_HOST_HEADER);
        String host = (forwardedHost != null && !forwardedHost.isBlank())
                ? forwardedHost : request.getServerName();
        String hostWithoutPort = host.split(":")[0];
        String[] parts = hostWithoutPort.split("\\.");
        if (parts.length < 3) {
            return null;
        }
        String subdomain = parts[0].toLowerCase(Locale.ROOT);
        if (RESERVED_SUBDOMAINS.contains(subdomain)) {
            return null;
        }
        return subdomain;
    }

    /**
     * Looks up the tenant slug for a given subdomain.
     * Returns null if no tenant has this subdomain registered.
     */
    String findSlugBySubdomain(String subdomain) {
        String sql = "SELECT slug FROM public.tenants WHERE subdomain = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, subdomain);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getString(1) : null;
            }
        } catch (SQLException ex) {
            log.error("Tenant subdomain lookup query failed", ex);
            return null;
        }
    }

    private boolean isBootstrapRequest(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/platform/v1/bootstrap") || uri.startsWith("/api/platform/v1/setup");
    }

    private boolean tenantExists(String slug) {
        String sql = "SELECT 1 FROM public.tenants WHERE slug = ?";
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, slug);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        } catch (SQLException ex) {
            log.error("Tenant slug validation query failed", ex);
            throw new AccessDeniedException("Tenant validation failed.");
        }
    }
}