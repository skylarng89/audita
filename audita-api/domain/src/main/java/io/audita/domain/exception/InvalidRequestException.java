package io.audita.domain.exception;

public class InvalidRequestException extends DomainException {

    private final String errorCode;

    public InvalidRequestException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
