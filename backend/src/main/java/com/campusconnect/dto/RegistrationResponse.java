package com.campusconnect.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import com.campusconnect.model.Event;
import com.campusconnect.model.EventCategory;
import com.campusconnect.model.EventStatus;
import com.campusconnect.model.Registration;
import com.campusconnect.model.RegistrationStatus;

/**
 * A registration joined with the event summary the UI needs.
 *
 * <p>Returning the joined shape keeps "My Registrations" and the QR pass from
 * having to issue a follow-up request per row.
 */
public record RegistrationResponse(
        String registrationId,
        String qrToken,
        RegistrationStatus status,
        Instant registeredAt,
        Instant cancelledAt,
        String eventId,
        String eventTitle,
        EventCategory category,
        String department,
        LocalDate eventDate,
        LocalTime startTime,
        LocalTime endTime,
        String venue,
        EventStatus eventStatus) {

    public static RegistrationResponse from(Registration registration, Event event) {
        return new RegistrationResponse(
                registration.getId(),
                registration.getQrToken(),
                registration.getStatus(),
                registration.getRegisteredAt(),
                registration.getCancelledAt(),
                event.getId(),
                event.getTitle(),
                event.getCategory(),
                event.getDepartment(),
                event.getDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.getVenue(),
                event.getStatus());
    }
}
