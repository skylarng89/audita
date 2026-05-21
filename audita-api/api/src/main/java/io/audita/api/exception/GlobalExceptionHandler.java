package io.audita.api.exception;

import io.audita.domain.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Translates domain and framework exceptions into RFC 7807 Problem Detail responses.
 * All error responses follow a consistent shape.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        detail.setTitle("Resource Not Found");
        detail.setType(URI.create("https://audita.io/errors/not-found"));
        return detail;
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ProblemDetail handleInvalidState(InvalidStateTransitionException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        detail.setTitle("Invalid State Transition");
        detail.setType(URI.create("https://audita.io/errors/invalid-state"));
        if (ex.getErrorCode() != null) {
            detail.setProperty("errorCode", ex.getErrorCode());
        }
        return detail;
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ProblemDetail handleInvalidRequest(InvalidRequestException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setTitle("Invalid Request");
        detail.setType(URI.create("https://audita.io/errors/invalid-request"));
        detail.setProperty("errorCode", ex.getErrorCode());
        return detail;
    }

    @ExceptionHandler(DomainNotPermittedException.class)
    public ProblemDetail handleDomainNotPermitted(DomainNotPermittedException ex,
                                                  HttpServletRequest request) {
        if (log.isWarnEnabled()) {
            String method = request.getMethod();
            String path = request.getRequestURI();
            String origin = request.getHeader("Origin");
            String referer = request.getHeader("Referer");
            boolean authHeaderPresent = request.getHeader("Authorization") != null;
            boolean cookiePresent = request.getHeader("Cookie") != null;
            boolean tenantHeaderPresent = request.getHeader("X-Tenant-Slug") != null;
            log.warn("DomainNotPermitted: method={} path={} errorCode={} message={} origin={} referer={} authHeaderPresent={} cookiePresent={} tenantHeaderPresent={}",
                method,
                path,
                ex.getErrorCode(),
                ex.getMessage(),
                origin,
                referer,
                authHeaderPresent,
                cookiePresent,
                tenantHeaderPresent);
        }
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        detail.setTitle("Action Not Permitted");
        detail.setType(URI.create("https://audita.io/errors/not-permitted"));
        detail.setProperty("errorCode", ex.getErrorCode());
        return detail;
    }

    @ExceptionHandler(DomainException.class)
    public ProblemDetail handleDomain(DomainException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setTitle("Business Rule Violation");
        detail.setType(URI.create("https://audita.io/errors/domain-error"));
        return detail;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex,
                                            HttpServletRequest request) {
        if (log.isWarnEnabled()) {
            String method = request.getMethod();
            String path = request.getRequestURI();
            String origin = request.getHeader("Origin");
            String referer = request.getHeader("Referer");
            boolean authHeaderPresent = request.getHeader("Authorization") != null;
            boolean cookiePresent = request.getHeader("Cookie") != null;
            boolean tenantHeaderPresent = request.getHeader("X-Tenant-Slug") != null;
            log.warn("AccessDenied: method={} path={} message={} origin={} referer={} authHeaderPresent={} cookiePresent={} tenantHeaderPresent={}",
                method,
                path,
                ex.getMessage(),
                origin,
                referer,
                authHeaderPresent,
                cookiePresent,
                tenantHeaderPresent);
        }
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied.");
        detail.setTitle("Forbidden");
        detail.setType(URI.create("https://audita.io/errors/forbidden"));
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid",
                        (a, b) -> a
                ));
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
            HttpStatus.UNPROCESSABLE_CONTENT, "Validation failed.");
        detail.setTitle("Validation Error");
        detail.setType(URI.create("https://audita.io/errors/validation"));
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unhandled exception", ex);
        // Intentionally vague — do not expose stack traces
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
        detail.setTitle("Internal Server Error");
        detail.setType(URI.create("https://audita.io/errors/internal"));
        return detail;
    }
}
