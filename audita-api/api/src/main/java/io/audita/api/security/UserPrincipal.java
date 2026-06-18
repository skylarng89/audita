package io.audita.api.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.LinkedHashSet;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Spring Security principal for authenticated users (both tenant users and
 * Super Admin).
 * Loaded from the validated JWT on each request.
 */
public record UserPrincipal(
        UUID userId,
        String email,
        String role,
        List<String> roles,
        List<String> permissions,
        String tenantSlug, // null for Super Admin
        boolean isSuperAdmin,
        Collection<? extends GrantedAuthority> authorities)implements UserDetails {

    private static final String WILDCARD = "*";

    public static UserPrincipal ofTenantUser(
            UUID userId,
            String email,
            String role,
            List<String> roles,
            List<String> permissions,
            String tenantSlug) {
        List<String> normalizedRoles = (roles == null || roles.isEmpty())
                ? List.of(role)
                : roles;
        LinkedHashSet<GrantedAuthority> authorities = new LinkedHashSet<>();
        for (String roleName : normalizedRoles) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + normalizeRole(roleName)));
        }
        if (permissions != null) {
            for (String permission : permissions) {
                if (permission != null && !permission.isBlank()) {
                    authorities.add(new SimpleGrantedAuthority(permission.trim().toLowerCase(Locale.ROOT)));
                }
            }
        }

        return new UserPrincipal(userId, email, role, normalizedRoles, permissions, tenantSlug, false, authorities);
    }

    public static UserPrincipal ofSuperAdmin(UUID userId, String email) {
        List<GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"),
                new SimpleGrantedAuthority(WILDCARD)
        );
        return new UserPrincipal(userId, email, "SUPER_ADMIN", List.of("SUPER_ADMIN"), List.of(), null, true,
                authorities);
    }

    private static String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "REQUESTER";
        }
        return role.trim().replace('-', '_').replace(' ', '_').toUpperCase();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return userId.toString();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
