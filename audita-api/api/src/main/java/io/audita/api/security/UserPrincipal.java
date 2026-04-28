package io.audita.api.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security principal for authenticated users (both tenant users and Super Admin).
 * Loaded from the validated JWT on each request.
 */
public record UserPrincipal(
        UUID userId,
        String email,
        String role,
        String tenantSlug,   // null for Super Admin
        boolean isSuperAdmin,
        Collection<? extends GrantedAuthority> authorities
) implements UserDetails {

    public static UserPrincipal ofTenantUser(UUID userId, String email, String role, String tenantSlug) {
        return new UserPrincipal(userId, email, role, tenantSlug, false,
                List.of(new SimpleGrantedAuthority("ROLE_" + role)));
    }

    public static UserPrincipal ofSuperAdmin(UUID userId, String email) {
        return new UserPrincipal(userId, email, "SUPER_ADMIN", null, true,
                List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public String getPassword() { return null; }
    @Override public String getUsername() { return userId.toString(); }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
