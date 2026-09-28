package com.campusconnect.exception;

import org.springframework.http.HttpStatus;

/**
 * The student already holds an active registration for this event.
 * Rendered as HTTP 409, matching the documented contract.
 */
public class DuplicateRegistrationException extends ApiException {

    public static final String DEFAULT_MESSAGE = "You are already registered for this event";

    public DuplicateRegistrationException() {
        this(DEFAULT_MESSAGE);
    }

    public DuplicateRegistrationException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
