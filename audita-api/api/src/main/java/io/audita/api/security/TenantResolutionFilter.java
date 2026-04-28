package io.audita.api.security;

import io.audita.infrastructure.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    private static final String TENANT_HEADER = "X-Tenant-Slug";
    private static final Pattern TENANT_SLUG_PATTERN = Pattern.compile("^[a-z0-9-]{1,100}$");

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String slug = request.getHeader(TENANT_HEADER);
        if (slug != null && !slug.isBlank()) {
            String normalizedSlug = slug.trim().toLowerCase(Locale.ROOT);
            if (!TENANT_SLUG_PATTERN.matcher(normalizedSlug).matches()) {
                throw new AccessDeniedException("Invalid tenant slug.");
            }
            TenantContext.setCurrentTenant(normalizedSlug);
        }
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
