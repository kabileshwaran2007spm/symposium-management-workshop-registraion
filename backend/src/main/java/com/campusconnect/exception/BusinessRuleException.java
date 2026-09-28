package com.campusconnect.exception;

import org.springframework.http.HttpStatus;

/**
 * A domain rule was violated: an illegal status transition, a registration
 * deadline that has passed, a cancellation after attendance, and so on.
 * Rendered as HTTP 409 because the request conflicts with current state.
 */
public class BusinessRuleException extends ApiException {

    public BusinessRuleException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
