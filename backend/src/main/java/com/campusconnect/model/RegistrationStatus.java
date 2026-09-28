package com.campusconnect.model;

import java.util.Set;

/**
 * Registration lifecycle status.
 *
 * <p>The planning documents also show a {@code PROMOTED} node in the waitlist
 * flow, but the canonical status list has four values and no {@code PROMOTED}.
 * Promotion is a <em>transition</em> that ends in {@code CONFIRMED}, not a
 * stored state.
 */
public enum RegistrationStatus {
    CONFIRMED,
    CANCELLED,
    WAITLISTED,
    ATTENDED;

    /**
     * Statuses that occupy a seat. These are the values the partial unique
     * index is built on, so a student may hold at most one active
     * registration per event while still being able to re-register after
     * cancelling.
     */
    public static final Set<RegistrationStatus> SEAT_HOLDING =
            Set.of(CONFIRMED, WAITLISTED, ATTENDED);

    /** Values persisted in the partial unique index filter, as strings. */
    public static final Set<String> SEAT_HOLDING_NAMES =
            Set.of(CONFIRMED.name(), WAITLISTED.name(), ATTENDED.name());

    public boolean isSeatHolding() {
        return SEAT_HOLDING.contains(this);
    }
}
