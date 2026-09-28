package com.campusconnect.dto;

/**
 * Seat-availability filter exposed by the event search UI.
 *
 * <p>These are not stored values. Availability is derived from
 * {@code registeredCount} against {@code capacity} at query time, which is why
 * the repository translates them into a MongoDB {@code $expr} comparison instead
 * of an equality match.
 */
public enum EventAvailability {

    /** Seats remain and the event is not close to capacity. */
    AVAILABLE,

    /** Seats remain but the event is nearly full. */
    ALMOST_FULL,

    /** No seats remain. */
    FULL
}
