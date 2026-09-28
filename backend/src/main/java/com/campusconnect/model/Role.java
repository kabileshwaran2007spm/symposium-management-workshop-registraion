package com.campusconnect.model;

/**
 * The three primary roles defined by the product requirements.
 *
 * <p>{@code STUDENT} is the only role that can be created through public
 * registration. {@code ORGANIZER} and {@code ADMIN} accounts are provisioned
 * by an administrator, which keeps self-service signup from becoming a
 * privilege-escalation path.
 */
public enum Role {
    STUDENT,
    ORGANIZER,
    ADMIN
}
