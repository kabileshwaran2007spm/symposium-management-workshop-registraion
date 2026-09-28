package com.campusconnect.exception;

import org.springframework.http.HttpStatus;

/**
 * No seats remain. Rendered as HTTP 409.
 *
 * <p>Waitlisting is part of the deferred Phase 8 work, so a full event is a hard
 * conflict for now rather than a waitlist entry.
 */
public class EventFullException extends ApiException {

    public EventFullException() {
        super(HttpStatus.CONFLICT, "Event is full");
    }
}
