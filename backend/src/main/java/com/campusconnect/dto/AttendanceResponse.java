package com.campusconnect.dto;

import java.time.Instant;

import com.campusconnect.model.Attendance;
import com.campusconnect.model.AttendanceStatus;
import com.campusconnect.model.User;

/** One attendance record joined with the student it belongs to. */
public record AttendanceResponse(
        String attendanceId,
        String registrationId,
        String userId,
        String studentId,
        String studentName,
        String email,
        Instant checkedInAt,
        AttendanceStatus status) {

    public static AttendanceResponse from(Attendance attendance, User user) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getRegistrationId(),
                attendance.getUserId(),
                user == null ? null : user.getStudentId(),
                user == null ? "Unknown student" : user.getName(),
                user == null ? null : user.getEmail(),
                attendance.getCheckedInAt(),
                attendance.getStatus());
    }
}
