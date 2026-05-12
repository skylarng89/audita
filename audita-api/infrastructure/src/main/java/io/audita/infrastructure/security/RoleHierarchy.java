package io.audita.infrastructure.security;

import io.audita.infrastructure.persistence.entity.RoleEntity;

import java.util.Collection;
import java.util.Comparator;
import java.util.Locale;

public final class RoleHierarchy {

    private RoleHierarchy() {
    }

    public static String highestRoleNameOrDefault(Collection<RoleEntity> roles, String fallback) {
        return roles.stream()
                .map(RoleEntity::getName)
                .filter(name -> name != null && !name.isBlank())
                .max(Comparator.comparingInt(RoleHierarchy::precedence)
                        .thenComparing(name -> name.toLowerCase(Locale.ROOT)))
                .orElse(fallback);
    }

    public static int precedence(String roleName) {
        if (roleName == null) {
            return 0;
        }
        String normalized = roleName.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "SUPER_ADMIN" -> 1000 ;
            case "ADMIN" -> 900 ;
            case "AUDITOR" -> 700 ;
            case "APPROVER" -> 500 ;
            case "REQUESTER" -> 300 ;
            default -> 100 ;
        };
    }
}