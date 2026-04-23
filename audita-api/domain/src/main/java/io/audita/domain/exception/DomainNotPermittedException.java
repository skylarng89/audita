package io.audita.domain.exception;

/**
 * Raised when a business rule prohibits an action — distinct from an authorization failure.
 * Example: domain whitelist blocks a login attempt.
 */
public class DomainNotPermittedException extends DomainException {

    private final String errorCode;

    public DomainNotPermittedException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
