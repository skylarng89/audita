package io.audita.domain.exception;

/**
 * Base for all domain rule violations.
 * These represent programmer or business-rule errors — not user input errors.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}
