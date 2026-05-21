package io.audita.domain.exception;

public class InvalidStateTransitionException extends DomainException {

    private final String errorCode;

    public InvalidStateTransitionException(String message) {
        super(message);
        this.errorCode = null;
    }

    public InvalidStateTransitionException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
