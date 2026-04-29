package io.audita.api.security;

import io.audita.infrastructure.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
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

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        boolean bootstrapRequest = isBootstrapRequest(request);
        String slug = request.getHeader(TENANT_HEADER);
        if (slug != null && !slug.isBlank()) {
            String normalizedSlug = slug.trim().toLowerCase(Locale.ROOT);
            if (!TENANT_SLUG_PATTERN.matcher(normalizedSlug).matches()) {
                log.warn("Rejected request due to invalid tenant slug: method={} path={} rawTenantSlug={} origin={} referer={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        slug,
                        request.getHeader("Origin"),
                        request.getHeader("Referer"));
                throw new AccessDeniedException("Invalid tenant slug.");
            }
            if (bootstrapRequest) {
                log.warn("Bootstrap request contains tenant header: method={} path={} tenantSlug={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        normalizedSlug);
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
}
