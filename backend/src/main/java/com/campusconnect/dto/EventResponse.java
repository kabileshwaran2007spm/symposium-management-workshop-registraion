package com.campusconnect.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import com.campusconnect.model.Event;
import com.campusconnect.model.EventCategory;
import com.campusconnect.model.EventStatus;

/**
 * Client-facing event view.
 *
 * <p>{@code availableSeats}, {@code full} and {@code almostFull} are computed
 * from {@code capacity - registeredCount} on every read, never stored, so they
 * cannot drift away from the seat count.
 *
 * <p>{@code registrationOpen} answers the question the details page actually
 * asks — "can I register right now?" — combining status, deadline and capacity so
 * the UI does not have to re-derive the business rules.
 */
public record EventResponse(
        String id,
        String title,
        String description,
        EventCategory category,
        String department,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        String venue,
        int capacity,
        int registeredCount,
        int availableSeats,
        boolean full,
        boolean almostFull,
        LocalDate registrationDeadline,
        EventStatus status,
        boolean registrationOpen,
        String organizerId,
        String organizerName,
        Instant createdAt,
        Instant updatedAt) {

    public static EventResponse from(Event event, String organizerName, boolean registrationOpen) {
        return new EventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getCategory(),
                event.getDepartment(),
                event.getDate(),
                event.getStartTime(),
                event.getEndTime(),
                event.getVenue(),
                event.getCapacity(),
                event.getRegisteredCount(),
                event.getAvailableSeats(),
                event.isFull(),
                event.isAlmostFull(),
                event.getRegistrationDeadline(),
                event.getStatus(),
                registrationOpen,
                event.getOrganizerId(),
                organizerName,
                event.getCreatedAt(),
                event.getUpdatedAt());
    }

    /** Convenience for listings, where the extra registration check is not needed. */
    public static EventResponse from(Event event, String organizerName) {
        return from(event, organizerName, false);
    }
}
