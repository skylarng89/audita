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
import java.util.regex.Pattern;

/**
 * Resolves the active tenant from the {@code X-Tenant-Slug} request header and
 * stores it in {@link TenantContext} for the duration of the request.
 *
 * Must run BEFORE {@code JwtAuthenticationFilter} so that unauthenticated
 * endpoints (login, refresh, SSO callbacks) can also resolve the correct
 * tenant schema for their DB queries.
 */
@Component
public class TenantResolutionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantResolutionFilter.class);

    private static final String TENANT_HEADER = "X-Tenant-Slug";
    private static final Pattern TENANT_SLUG_PATTERN = Pattern.compile("^[a-z0-9-]{1,100}$");
    private final DataSource dataSource;

    public TenantResolutionFilter(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        boolean bootstrapRequest = isBootstrapRequest(request);
        String slug = request.getHeader(TENANT_HEADER);
        if (slug != null && !slug.isBlank()) {
            String normalizedSlug = slug.trim().toLowerCase(Locale.ROOT);
            if (!TENANT_SLUG_PATTERN.matcher(normalizedSlug).matches()) {
                if (log.isWarnEnabled()) {
                    String origin = request.getHeader("Origin");
                    String referer = request.getHeader("Referer");
                    log.warn("Rejected request due to invalid tenant slug: method={} path={} rawTenantSlug={} origin={} referer={}",
                            request.getMethod(),
                            request.getRequestURI(),
                            slug,
                            origin,
                            referer);
                }
                throw new AccessDeniedException("Invalid tenant slug.");
            }
            if (bootstrapRequest) {
                log.warn("Rejected bootstrap request containing tenant header: method={} path={} tenantSlug={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        normalizedSlug);
                throw new AccessDeniedException("Tenant header is not allowed for bootstrap requests.");
            }
            if (!tenantExists(normalizedSlug)) {
                log.warn("Rejected request due to unknown tenant slug: method={} path={} tenantSlug={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        normalizedSlug);
                throw new AccessDeniedException("Unknown tenant.");
            }
            TenantContext.setCurrentTenant(normalizedSlug);
        } else if (bootstrapRequest) {
            log.info("Bootstrap request has no tenant header: method={} path={}",
                    request.getMethod(),
                    request.getRequestURI());
        }
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
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
