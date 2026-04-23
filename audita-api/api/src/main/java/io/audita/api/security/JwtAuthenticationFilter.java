package io.audita.api.security;

import io.audita.infrastructure.security.JwtService;
import io.audita.infrastructure.tenant.TenantContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Validates the Bearer token on every request.
 * On success: populates SecurityContext + TenantContext.
 * On failure: continues the filter chain unauthenticated (Spring Security handles 401).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);

        if (!jwtService.isValid(token)) {
            chain.doFilter(request, response);
            return;
        }

        Claims claims = jwtService.parse(token);
        UUID userId = UUID.fromString(claims.getSubject());
        String email = claims.get("email", String.class);
        String role = claims.get("role", String.class);
        String tenantSlug = claims.get("tenantSlug", String.class);

        UserPrincipal principal;
        if ("SUPER_ADMIN".equals(role)) {
            principal = UserPrincipal.ofSuperAdmin(userId, email);
        } else {
            principal = UserPrincipal.ofTenantUser(userId, email, role, tenantSlug);
            // Set tenant schema context for this request
            if (tenantSlug != null && !tenantSlug.isBlank()) {
                TenantContext.setCurrentTenant(tenantSlug);
            }
        }

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(request, response);
        // Clear tenant context after request completes to prevent thread-pool bleed
        TenantContext.clear();
    }
}
