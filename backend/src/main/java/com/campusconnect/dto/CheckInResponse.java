package com.campusconnect.dto;

import java.time.Instant;

/** Confirmation returned to the scanner after a successful check-in. */
public record CheckInResponse(
        String attendanceId,
        String registrationId,
        String eventId,
        String eventTitle,
        String userId,
        String studentName,
        String studentId,
        Instant checkedInAt,
        long checkedInCount,
        long remainingCount) {
}
