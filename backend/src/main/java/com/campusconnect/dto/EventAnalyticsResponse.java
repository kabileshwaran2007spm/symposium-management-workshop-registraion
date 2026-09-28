package com.campusconnect.dto;

/**
 * Per-event analytics, matching the metrics listed in the implementation
 * document: capacity, registrations, available seats, attendance, attendance
 * percentage and cancellations.
 *
 * <p>{@code attendancePercentage} is attendance divided by registrations, as in
 * the documented worked example (150 / 180 = 83.33%). It is zero rather than
 * {@code NaN} when nobody has registered.
 */
public record EventAnalyticsResponse(
        String eventId,
        String eventTitle,
        int capacity,
        int registrations,
        int availableSeats,
        long confirmed,
        long waitlisted,
        long cancelled,
        long attended,
        long attendanceCount,
        double attendancePercentage) {

    public static EventAnalyticsResponse of(
            String eventId,
            String eventTitle,
            int capacity,
            int registrations,
            int availableSeats,
            long confirmed,
            long waitlisted,
            long cancelled,
            long attended,
            long attendanceCount) {

        double percentage = registrations == 0
                ? 0.0
                : Math.round((attendanceCount * 10000.0) / registrations) / 100.0;

        return new EventAnalyticsResponse(
                eventId, eventTitle, capacity, registrations, availableSeats,
                confirmed, waitlisted, cancelled, attended, attendanceCount, percentage);
    }
}
