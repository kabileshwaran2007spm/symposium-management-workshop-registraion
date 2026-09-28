package com.campusconnect.model;

import java.util.Set;

/**
 * Event lifecycle status.
 *
 * <p>The planning documents use two different vocabularies: an implementation
 * diagram that shows {@code APPROVED -> PUBLISHED -> COMPLETED}, and the
 * canonical status list, which has six values and no {@code PUBLISHED}. The
 * six-value list wins, and "published" is treated as the <em>meaning</em> of
 * {@code APPROVED} (an approved event is the one students can see and register
 * for) rather than a separate stored value.
 */
public enum EventStatus {
    DRAFT,
    PENDING,
    APPROVED,
    REJECTED,
    CANCELLED,
    COMPLETED;

    /** Statuses a student may discover and register for. */
    public static final Set<EventStatus> STUDENT_VISIBLE = Set.of(APPROVED);

    /** Statuses that no longer accept registrations. */
    public static final Set<EventStatus> CLOSED_FOR_REGISTRATION =
            Set.of(DRAFT, PENDING, REJECTED, CANCELLED, COMPLETED);

    /**
     * Statuses an organizer may legally move an event to. Enforced by
     * {@code EventService} so the state machine lives in one place.
     */
    public boolean canTransitionTo(EventStatus target) {
        return switch (this) {
            case DRAFT -> target == PENDING || target == CANCELLED;
            case PENDING -> target == APPROVED || target == REJECTED || target == DRAFT;
            case REJECTED -> target == PENDING;
            case APPROVED -> target == CANCELLED || target == COMPLETED;
            case CANCELLED, COMPLETED -> false;
        };
    }
}
