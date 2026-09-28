package com.campusconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DuplicateKeyException;

import com.campusconnect.dto.AttendanceResponse;
import com.campusconnect.dto.CheckInResponse;
import com.campusconnect.exception.BusinessRuleException;
import com.campusconnect.exception.ForbiddenException;
import com.campusconnect.exception.ResourceNotFoundException;
import com.campusconnect.model.Attendance;
import com.campusconnect.model.Event;
import com.campusconnect.model.EventCategory;
import com.campusconnect.model.EventStatus;
import com.campusconnect.model.Registration;
import com.campusconnect.model.RegistrationStatus;
import com.campusconnect.model.Role;
import com.campusconnect.model.User;
import com.campusconnect.repository.AttendanceRepository;
import com.campusconnect.repository.RegistrationRepository;
import com.campusconnect.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttendanceServiceTest {

    private static final String ORGANIZER_ID = "organizer-1";
    private static final String OTHER_ORGANIZER_ID = "organizer-2";
    private static final String QR_TOKEN = "qr-token-abc";
    private static final String EVENT_ID = "event-1";
    private static final String STUDENT_ID = "student-1";

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventService eventService;

    private AttendanceService attendanceService;

    @BeforeEach
    void setUp() {
        attendanceService = new AttendanceService(
                attendanceRepository, registrationRepository, userRepository, eventService);
    }

    private Registration confirmedRegistration() {
        Registration registration = new Registration();
        registration.setId("registration-1");
        registration.setUserId(STUDENT_ID);
        registration.setEventId(EVENT_ID);
        registration.setQrToken(QR_TOKEN);
        registration.setStatus(RegistrationStatus.CONFIRMED);
        return registration;
    }

    private Event event() {
        Event event = new Event();
        event.setId(EVENT_ID);
        event.setTitle("AI Workshop");
        event.setCategory(EventCategory.WORKSHOP);
        event.setDepartment("CSE");
        event.setDate(LocalDate.now());
        event.setStartTime(LocalTime.of(10, 0));
        event.setEndTime(LocalTime.of(13, 0));
        event.setVenue("Seminar Hall");
        event.setCapacity(200);
        event.setRegisteredCount(120);
        event.setRegistrationDeadline(LocalDate.now());
        event.setStatus(EventStatus.APPROVED);
        event.setOrganizerId(ORGANIZER_ID);
        return event;
    }

    private User student() {
        User user = new User();
        user.setId(STUDENT_ID);
        user.setName("Arun Kumar");
        user.setStudentId("CS2026001");
        user.setEmail("arun@example.com");
        user.setDepartment("CSE");
        user.setRole(Role.STUDENT);
        return user;
    }

    @Test
    @DisplayName("a valid QR scan records attendance and marks the registration ATTENDED")
    void successfulCheckIn() {
        when(registrationRepository.findByQrToken(QR_TOKEN)).thenReturn(Optional.of(confirmedRegistration()));
        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(event());
        when(attendanceRepository.existsByRegistrationId("registration-1")).thenReturn(false);
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> {
            Attendance attendance = invocation.getArgument(0);
            attendance.setId("attendance-1");
            return attendance;
        });
        when(registrationRepository.save(any(Registration.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
        when(attendanceRepository.countByEventId(EVENT_ID)).thenReturn(1L);

        CheckInResponse response = attendanceService.checkIn(ORGANIZER_ID, Role.ORGANIZER, QR_TOKEN);

        assertThat(response.attendanceId()).isEqualTo("attendance-1");
        assertThat(response.studentName()).isEqualTo("Arun Kumar");
        assertThat(response.studentId()).isEqualTo("CS2026001");
        assertThat(response.checkedInCount()).isEqualTo(1L);
        assertThat(response.remainingCount()).isEqualTo(119L);

        verify(registrationRepository).save(any(Registration.class));
    }

    @Test
    @DisplayName("scanning the same pass twice is refused and does not create a second record")
    void duplicateCheckInIsRefused() {
        when(registrationRepository.findByQrToken(QR_TOKEN)).thenReturn(Optional.of(confirmedRegistration()));
        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(event());
        when(attendanceRepository.existsByRegistrationId("registration-1")).thenReturn(true);

        assertThatThrownBy(() -> attendanceService.checkIn(ORGANIZER_ID, Role.ORGANIZER, QR_TOKEN))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already been checked in");

        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    @DisplayName("the database index still stops a duplicate that slips past the pre-check")
    void duplicateKeyIsTranslatedToAConflict() {
        when(registrationRepository.findByQrToken(QR_TOKEN)).thenReturn(Optional.of(confirmedRegistration()));
        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(event());
        when(attendanceRepository.existsByRegistrationId("registration-1")).thenReturn(false);
        when(attendanceRepository.save(any(Attendance.class)))
                .thenThrow(new DuplicateKeyException("uniq_attendance_registration"));

        assertThatThrownBy(() -> attendanceService.checkIn(ORGANIZER_ID, Role.ORGANIZER, QR_TOKEN))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already been checked in");
    }

    @Test
    @DisplayName("an unknown QR token is a 404")
    void invalidTokenIsNotFound() {
        when(registrationRepository.findByQrToken(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.checkIn(ORGANIZER_ID, Role.ORGANIZER, "bogus"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Invalid QR pass");
    }

    @Test
    @DisplayName("a cancelled registration cannot be checked in")
    void cancelledRegistrationCannotCheckIn() {
        Registration cancelled = confirmedRegistration();
        cancelled.setStatus(RegistrationStatus.CANCELLED);
        when(registrationRepository.findByQrToken(QR_TOKEN)).thenReturn(Optional.of(cancelled));
        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(event());

        assertThatThrownBy(() -> attendanceService.checkIn(ORGANIZER_ID, Role.ORGANIZER, QR_TOKEN))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("has been cancelled");

        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    @DisplayName("an organizer cannot check students into somebody else's event")
    void foreignOrganizerCannotCheckIn() {
        when(registrationRepository.findByQrToken(QR_TOKEN)).thenReturn(Optional.of(confirmedRegistration()));
        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(event());

        org.mockito.Mockito.doThrow(new ForbiddenException("You can only manage events that you created"))
                .when(eventService).requireOwnership(any(Event.class), org.mockito.ArgumentMatchers.eq(OTHER_ORGANIZER_ID));

        assertThatThrownBy(() -> attendanceService.checkIn(OTHER_ORGANIZER_ID, Role.ORGANIZER, QR_TOKEN))
                .isInstanceOf(ForbiddenException.class);

        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    @DisplayName("an administrator may check in for any event")
    void adminCanCheckInAnywhere() {
        when(registrationRepository.findByQrToken(QR_TOKEN)).thenReturn(Optional.of(confirmedRegistration()));
        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(event());
        when(attendanceRepository.existsByRegistrationId("registration-1")).thenReturn(false);
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(i -> i.getArgument(0));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(i -> i.getArgument(0));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
        when(attendanceRepository.countByEventId(EVENT_ID)).thenReturn(1L);

        CheckInResponse response = attendanceService.checkIn("admin-1", Role.ADMIN, QR_TOKEN);

        assertThat(response.eventId()).isEqualTo(EVENT_ID);
        verify(eventService, never()).requireOwnership(any(Event.class), anyString());
    }

    @Test
    @DisplayName("the attendance list joins each record to its student")
    void attendanceListJoinsStudents() {
        Attendance attendance = new Attendance();
        attendance.setId("attendance-1");
        attendance.setRegistrationId("registration-1");
        attendance.setEventId(EVENT_ID);
        attendance.setUserId(STUDENT_ID);
        attendance.setStatus(com.campusconnect.model.AttendanceStatus.PRESENT);

        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(event());
        when(attendanceRepository.findByEventIdOrderByCheckedInAtAsc(EVENT_ID)).thenReturn(List.of(attendance));
        when(userRepository.findByIdIn(List.of(STUDENT_ID))).thenReturn(List.of(student()));

        List<AttendanceResponse> rows = attendanceService.listAttendance(EVENT_ID, ORGANIZER_ID, Role.ORGANIZER);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).studentName()).isEqualTo("Arun Kumar");
        assertThat(rows.get(0).status()).isEqualTo(com.campusconnect.model.AttendanceStatus.PRESENT);
    }

    @Test
    @DisplayName("attendance export lists the checked-in students")
    void attendanceCsvExport() {
        Attendance attendance = new Attendance();
        attendance.setId("attendance-1");
        attendance.setRegistrationId("registration-1");
        attendance.setEventId(EVENT_ID);
        attendance.setUserId(STUDENT_ID);
        attendance.setCheckedInAt(java.time.Instant.parse("2026-10-12T10:15:00Z"));
        attendance.setStatus(com.campusconnect.model.AttendanceStatus.PRESENT);

        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(event());
        when(attendanceRepository.findByEventIdOrderByCheckedInAtAsc(EVENT_ID)).thenReturn(List.of(attendance));
        when(userRepository.findByIdIn(List.of(STUDENT_ID))).thenReturn(List.of(student()));

        String csv = attendanceService.exportCsv(EVENT_ID, ORGANIZER_ID, Role.ORGANIZER);
        List<String> lines = csv.lines().toList();

        assertThat(lines.get(0)).isEqualTo("Student ID,Name,Email,Checked In At,Status");
        assertThat(lines.get(1)).isEqualTo("CS2026001,Arun Kumar,arun@example.com,2026-10-12T10:15:00Z,PRESENT");
    }
}
