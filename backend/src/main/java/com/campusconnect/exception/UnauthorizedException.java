package com.campusconnect.exception;

import org.springframework.http.HttpStatus;

/** No usable authentication was present. Rendered as HTTP 401. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
