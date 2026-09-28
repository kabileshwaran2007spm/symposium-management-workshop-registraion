package com.campusconnect.exception;

import org.springframework.http.HttpStatus;

/**
 * The caller is authenticated but not allowed to touch this resource, for
 * example an organizer editing an event they do not own. Rendered as HTTP 403.
 */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
