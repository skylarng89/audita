package io.audita.api.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 * Convenience annotation to inject the authenticated UserPrincipal
 * into controller method parameters.
 *
 * Usage: public ResponseEntity<?> create(@CurrentUser UserPrincipal actor)
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
}
