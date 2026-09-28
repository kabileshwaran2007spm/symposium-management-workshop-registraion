package com.campusconnect.dto;

import java.time.Instant;

import com.campusconnect.model.Registration;
import com.campusconnect.model.RegistrationStatus;
import com.campusconnect.model.User;

/**
 * One row of the attendee table: student identity joined with their registration
 * status. Cancelled registrations are included so the record stays complete.
 */
public record AttendeeResponse(
        String registrationId,
        String userId,
        String studentId,
        String name,
        String email,
        String department,
        RegistrationStatus status,
        Instant registeredAt,
        boolean checkedIn) {

    public static AttendeeResponse from(Registration registration, User user, boolean checkedIn) {
        return new AttendeeResponse(
                registration.getId(),
                registration.getUserId(),
                user == null ? null : user.getStudentId(),
                user == null ? "Unknown student" : user.getName(),
                user == null ? null : user.getEmail(),
                user == null ? null : user.getDepartment(),
                registration.getStatus(),
                registration.getRegisteredAt(),
                checkedIn);
    }

    /** Flat export shape shared by the JSON export. */
    public ExportRow toExportRow() {
        return new ExportRow(studentId, name, email, department, status == null ? null : status.name());
    }

    /** Column set for the CSV export, matching the documented example. */
    public record ExportRow(String studentId, String name, String email, String department, String status) {
    }
}
