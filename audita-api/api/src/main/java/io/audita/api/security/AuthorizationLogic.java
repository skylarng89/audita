package io.audita.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collection;

/**
 * Spring component registered under the bean name {@code authz} so it can be
 * referenced from {@code @PreAuthorize} SpEL expressions such as
 * {@code @authz.hasPermission(authentication, 'permission.code')}.
 *
 * <p>Authorities are granted by {@link UserPrincipal}: tenant users receive
 * their permission codes as individual authorities, while Super Admin receives
 * the {@code *} wildcard which satisfies every permission check.
 */
@Component("authz")
public class AuthorizationLogic {

    private static final String WILDCARD = "*";

    public boolean hasPermission(Authentication auth, String permission) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities == null) {
            return false;
        }
        for (GrantedAuthority authority : authorities) {
            String name = authority.getAuthority();
            if (WILDCARD.equals(name) || name.equals(permission)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasAnyPermission(Authentication auth, String... permissions) {
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        if (authorities == null) {
            return false;
        }
        for (GrantedAuthority authority : authorities) {
            String name = authority.getAuthority();
            if (WILDCARD.equals(name)) {
                return true;
            }
            for (String perm : permissions) {
                if (name.equals(perm)) {
                    return true;
                }
            }
        }
        return false;
    }
}
