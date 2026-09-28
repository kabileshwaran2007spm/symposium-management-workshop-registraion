package com.campusconnect.dto;

import java.time.Instant;

import com.campusconnect.model.Event;
import com.campusconnect.model.Registration;
import com.campusconnect.model.RegistrationStatus;
import com.campusconnect.model.User;

/** A registration as seen by an administrator: who, which event, and in what state. */
public record AdminRegistrationResponse(
        String registrationId,
        String eventId,
        String eventTitle,
        String userId,
        String studentName,
        String studentId,
        String email,
        RegistrationStatus status,
        Instant registeredAt) {

    public static AdminRegistrationResponse from(Registration registration, Event event, User user) {
        return new AdminRegistrationResponse(
                registration.getId(),
                registration.getEventId(),
                event == null ? null : event.getTitle(),
                registration.getUserId(),
                user == null ? "Unknown student" : user.getName(),
                user == null ? null : user.getStudentId(),
                user == null ? null : user.getEmail(),
                registration.getStatus(),
                registration.getRegisteredAt());
    }
}
