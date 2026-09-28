package com.campusconnect.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.dto.ApiResponse;
import com.campusconnect.dto.AttendanceResponse;
import com.campusconnect.dto.CheckInRequest;
import com.campusconnect.dto.CheckInResponse;
import com.campusconnect.security.SecurityUtils;
import com.campusconnect.service.AttendanceService;

import jakarta.validation.Valid;

/**
 * Attendance scanning.
 *
 * <p>Only organizers (for their own events) and administrators may check anyone in;
 * the ownership check happens in the service because that is where the event is
 * known.
 */
@RestController
@RequestMapping("/api")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/attendance/check-in")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<CheckInResponse>> checkIn(@Valid @RequestBody CheckInRequest request) {
        CheckInResponse response = attendanceService.checkIn(
                SecurityUtils.currentUserId(), SecurityUtils.currentRole(), request.qrToken());
        return ResponseEntity.ok(ApiResponse.ok("Attendance recorded", response));
    }

    @GetMapping("/events/{eventId}/attendance")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<AttendanceResponse>>> listAttendance(@PathVariable String eventId) {
        return ResponseEntity.ok(ApiResponse.ok("Attendance retrieved successfully",
                attendanceService.listAttendance(
                        eventId, SecurityUtils.currentUserId(), SecurityUtils.currentRole())));
    }

    @GetMapping("/events/{eventId}/attendance/export")
    @PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
    public ResponseEntity<String> exportAttendance(@PathVariable String eventId) {
        String csv = attendanceService.exportCsv(
                eventId, SecurityUtils.currentUserId(), SecurityUtils.currentRole());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"event-" + eventId + "-attendance.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv);
    }
}
