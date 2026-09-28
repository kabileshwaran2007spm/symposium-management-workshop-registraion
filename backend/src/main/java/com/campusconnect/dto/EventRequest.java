package com.campusconnect.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.campusconnect.model.EventCategory;
import com.campusconnect.model.EventStatus;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Event create/update payload.
 *
 * <p>Field-level rules live here; cross-field rules (end time after start time,
 * deadline on or before the event date, deadline not in the past) are enforced in
 * {@code EventService} so they can return precise messages.
 *
 * <p>{@code status} is optional and only {@code DRAFT} or {@code PENDING} are
 * honoured, matching the two buttons on the create-event screen: "Save Draft" and
 * "Submit for Approval". An organizer can never set {@code APPROVED} directly.
 */
public record EventRequest(

        @NotBlank(message = "Event title is required")
        @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters")
        String title,

        @NotBlank(message = "Description is required")
        @Size(min = 10, max = 4000, message = "Description must be between 10 and 4000 characters")
        String description,

        @NotNull(message = "Category is required")
        EventCategory category,

        @NotBlank(message = "Department is required")
        @Size(max = 60, message = "Department must be at most 60 characters")
        String department,

        @NotNull(message = "Event date is required")
        LocalDate date,

        @NotNull(message = "Start time is required")
        LocalTime startTime,

        @NotNull(message = "End time is required")
        LocalTime endTime,

        @NotBlank(message = "Venue is required")
        @Size(max = 150, message = "Venue must be at most 150 characters")
        String venue,

        @Min(value = 1, message = "Capacity must be at least 1")
        @Max(value = 100_000, message = "Capacity must be at most 100000")
        int capacity,

        @NotNull(message = "Registration deadline is required")
        LocalDate registrationDeadline,

        EventStatus status) {
}
