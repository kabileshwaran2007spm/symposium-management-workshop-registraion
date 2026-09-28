package com.campusconnect.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for expected, user-facing failures.
 *
 * <p>Carrying the status on the exception keeps the mapping in one place: the
 * global handler reads it and emits the standard error envelope, so services can
 * describe <em>what</em> went wrong without knowing about HTTP.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
