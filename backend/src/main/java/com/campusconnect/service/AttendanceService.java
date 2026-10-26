package com.campusconnect.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import com.campusconnect.dto.AttendanceResponse;
import com.campusconnect.dto.CheckInResponse;
import com.campusconnect.exception.BusinessRuleException;
import com.campusconnect.exception.ResourceNotFoundException;
import com.campusconnect.model.Attendance;
import com.campusconnect.model.AttendanceStatus;
import com.campusconnect.model.Event;
import com.campusconnect.model.Registration;
import com.campusconnect.model.RegistrationStatus;
import com.campusconnect.model.Role;
import com.campusconnect.model.User;
import com.campusconnect.repository.AttendanceRepository;
import com.campusconnect.repository.RegistrationRepository;
import com.campusconnect.repository.UserRepository;
import com.campusconnect.util.CsvWriter;

/**
 * Attendance via QR pass.
 *
 * <p>Check-in is idempotent in two layers: an explicit existence check gives the
 * organizer a friendly "already checked in" message, and the unique index on
 * {@code attendance.registrationId} guarantees that two simultaneous scans of the
 * same pass still produce one record.
 */
@Service
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);

    private static final List<String> EXPORT_HEADERS =
            List.of("Student ID", "Name", "Email", "Checked In At", "Status");

    private final AttendanceRepository attendanceRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;
    private final EventService eventService;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            RegistrationRepository registrationRepository,
            UserRepository userRepository,
            EventService eventService) {
        this.attendanceRepository = attendanceRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
        this.eventService = eventService;
    }

    public CheckInResponse checkIn(String requesterId, Role requesterRole, String qrToken) {
        Registration registration = registrationRepository.findByQrToken(qrToken)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid QR pass"));

        Event event = eventService.findEventOrThrow(registration.getEventId());
        if (requesterRole != Role.ADMIN) {
            eventService.requireOwnership(event, requesterId);
        }

        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new BusinessRuleException("This registration has been cancelled");
        }
        if (attendanceRepository.existsByRegistrationId(registration.getId())) {
            throw new BusinessRuleException("This student has already been checked in");
        }

        Attendance attendance = new Attendance();
        attendance.setRegistrationId(registration.getId());
        attendance.setEventId(event.getId());
        attendance.setUserId(registration.getUserId());
        attendance.setCheckedInAt(Instant.now());
        attendance.setStatus(AttendanceStatus.PRESENT);

        try {
            attendance = attendanceRepository.save(attendance);
        } catch (DuplicateKeyException ex) {
            // Two scans raced; the index kept the record singular.
            throw new BusinessRuleException("This student has already been checked in");
        }

        registration.setStatus(RegistrationStatus.ATTENDED);
        registrationRepository.save(registration);

        User student = userRepository.findById(registration.getUserId()).orElse(null);
        long checkedIn = attendanceRepository.countByEventId(event.getId());

        log.info("Checked in registration {} for event {}", registration.getId(), event.getId());

        return new CheckInResponse(
                attendance.getId(),
                registration.getId(),
                event.getId(),
                event.getTitle(),
                registration.getUserId(),
                student == null ? "Unknown student" : student.getName(),
                student == null ? null : student.getStudentId(),
                attendance.getCheckedInAt(),
                checkedIn,
                Math.max(0, event.getRegisteredCount() - checkedIn));
    }

    public List<AttendanceResponse> listAttendance(String eventId, String requesterId, Role requesterRole) {
        Event event = eventService.findEventOrThrow(eventId);
        if (requesterRole != Role.ADMIN) {
            eventService.requireOwnership(event, requesterId);
        }

        List<Attendance> records = attendanceRepository.findByEventIdOrderByCheckedInAtAsc(eventId);
        if (records.isEmpty()) {
            return List.of();
        }

        Map<String, User> users = userRepository.findByIdIn(records.stream()
                        .map(record -> record.getUserId())
                        .distinct()
                        .toList())
                .stream()
                .collect(Collectors.toMap(user -> user.getId(), user -> user, (a, b) -> a));

        return records.stream()
                .map(record -> AttendanceResponse.from(record, users.get(record.getUserId())))
                .toList();
    }

    public String exportCsv(String eventId, String requesterId, Role requesterRole) {
        List<AttendanceResponse> records = listAttendance(eventId, requesterId, requesterRole);

        List<List<String>> rows = records.stream()
                .map(record -> List.of(
                        nullToEmpty(record.studentId()),
                        nullToEmpty(record.studentName()),
                        nullToEmpty(record.email()),
                        record.checkedInAt() == null ? "" : record.checkedInAt().toString(),
                        record.status() == null ? "" : record.status().name()))
                .toList();

        return CsvWriter.toCsv(EXPORT_HEADERS, rows);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
