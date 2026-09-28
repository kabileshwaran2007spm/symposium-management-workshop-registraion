package com.campusconnect.exception;

import org.springframework.http.HttpStatus;

/** Requested resource does not exist. Rendered as HTTP 404. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
