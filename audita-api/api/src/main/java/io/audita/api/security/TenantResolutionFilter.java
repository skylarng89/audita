package io.audita.api.security;

import io.audita.infrastructure.tenant.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

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

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String slug = request.getHeader(TENANT_HEADER);
        if (slug != null && !slug.isBlank()) {
            TenantContext.setCurrentTenant(slug.trim().toLowerCase());
        }
        try {
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
