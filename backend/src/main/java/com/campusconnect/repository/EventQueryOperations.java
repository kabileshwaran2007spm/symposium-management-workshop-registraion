package com.campusconnect.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.campusconnect.dto.EventFilter;
import com.campusconnect.dto.EventRequest;
import com.campusconnect.model.Event;
import com.campusconnect.model.EventStatus;

/**
 * Event queries that need more than a derived query method.
 *
 * <p>Everything here is expressed with {@code MongoTemplate} for two reasons:
 * the seat operations must be atomic, and the availability filter compares two
 * document fields, which requires a server-side expression.
 */
public interface EventQueryOperations {

    /**
     * Atomically claims one seat on an approved event that still has room.
     *
     * @return the updated event, or empty when the seat could not be claimed
     *         (event gone, not approved, deadline passed, or full). Callers
     *         re-read the event to report the precise reason to the user.
     */
    Optional<Event> claimSeat(String eventId, int capacity, LocalDate today);

    /** Atomically releases a claimed seat, never dropping below zero. */
    boolean releaseSeat(String eventId);

    /** Filtered, paginated event listing. */
    Page<Event> search(EventFilter filter, Pageable pageable);

    /**
     * Updates organizer-editable event fields without replacing concurrent seat
     * claims. The status and capacity predicates make the transition atomic with
     * respect to concurrent approvals and registrations.
     */
    Optional<Event> updateEditableFields(
            String eventId,
            String organizerId,
            EventStatus expectedStatus,
            EventRequest request,
            EventStatus resultingStatus);
}
