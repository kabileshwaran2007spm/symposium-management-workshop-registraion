package com.campusconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DuplicateKeyException;

import com.campusconnect.dto.EventAnalyticsResponse;
import com.campusconnect.dto.RegistrationResponse;
import com.campusconnect.exception.BusinessRuleException;
import com.campusconnect.exception.DuplicateRegistrationException;
import com.campusconnect.exception.EventFullException;
import com.campusconnect.exception.ForbiddenException;
import com.campusconnect.exception.ResourceNotFoundException;
import com.campusconnect.model.Event;
import com.campusconnect.model.EventCategory;
import com.campusconnect.model.EventStatus;
import com.campusconnect.model.Registration;
import com.campusconnect.model.RegistrationStatus;
import com.campusconnect.model.Role;
import com.campusconnect.model.User;
import com.campusconnect.repository.AttendanceRepository;
import com.campusconnect.repository.EventRepository;
import com.campusconnect.repository.RegistrationRepository;
import com.campusconnect.repository.UserRepository;

/**
 * The registration rules and, in particular, the two guards that keep an event
 * from being oversold or double-booked.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegistrationServiceTest {

    private static final String STUDENT_ID = "student-1";
    private static final String EVENT_ID = "event-1";

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private EventService eventService;

    private RegistrationService registrationService;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationService(
                registrationRepository, eventRepository, userRepository, attendanceRepository, eventService);
    }

    private User student() {
        User user = new User();
        user.setId(STUDENT_ID);
        user.setName("Arun Kumar");
        user.setEmail("arun@example.com");
        user.setStudentId("CS2026001");
        user.setDepartment("CSE");
        user.setRole(Role.STUDENT);
        return user;
    }

    private Event openEvent(int capacity, int registered) {
        Event event = new Event();
        event.setId(EVENT_ID);
        event.setTitle("AI Workshop");
        event.setCategory(EventCategory.WORKSHOP);
        event.setDepartment("CSE");
        event.setDate(LocalDate.now().plusDays(10));
        event.setStartTime(LocalTime.of(10, 0));
        event.setEndTime(LocalTime.of(13, 0));
        event.setVenue("Seminar Hall");
        event.setCapacity(capacity);
        event.setRegisteredCount(registered);
        event.setRegistrationDeadline(LocalDate.now().plusDays(5));
        event.setStatus(EventStatus.APPROVED);
        event.setOrganizerId("organizer-1");
        return event;
    }

    private void existingStudent() {
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
    }

    /** Makes save() assign an id, the way MongoDB would. */
    private void savingAssignsId() {
        when(registrationRepository.save(any(Registration.class))).thenAnswer(invocation -> {
            Registration registration = invocation.getArgument(0);
            if (registration.getId() == null) {
                registration.setId("registration-1");
            }
            return registration;
        });
    }

    // ------------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a successful registration claims a seat and issues a QR token")
    void successfulRegistration() {
        existingStudent();
        Event event = openEvent(200, 120);
        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(event);
        when(registrationRepository.findActiveRegistration(STUDENT_ID, EVENT_ID)).thenReturn(Optional.empty());

        Event claimed = openEvent(200, 121);
        when(eventRepository.claimSeat(eq(EVENT_ID), eq(200), any(LocalDate.class)))
                .thenReturn(Optional.of(claimed));
        savingAssignsId();

        RegistrationResponse response = registrationService.register(STUDENT_ID, EVENT_ID);

        assertThat(response.status()).isEqualTo(RegistrationStatus.CONFIRMED);
        assertThat(response.eventTitle()).isEqualTo("AI Workshop");
        assertThat(response.registrationId()).isEqualTo("registration-1");
        assertThat(response.qrToken()).isNotBlank();

        ArgumentCaptor<Registration> saved = ArgumentCaptor.forClass(Registration.class);
        verify(registrationRepository).save(saved.capture());
        assertThat(saved.getValue().getRegisteredAt()).isNotNull();
    }

    @Test
    @DisplayName("registering twice hits 409 and never takes a second seat")
    void duplicateRegistrationIsRejected() {
        existingStudent();
        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(openEvent(200, 120));

        Registration existing = new Registration();
        existing.setId("registration-existing");
        existing.setStatus(RegistrationStatus.CONFIRMED);
        when(registrationRepository.findActiveRegistration(STUDENT_ID, EVENT_ID))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> registrationService.register(STUDENT_ID, EVENT_ID))
                .isInstanceOf(DuplicateRegistrationException.class)
                .hasMessage("You are already registered for this event");

        verify(eventRepository, never()).claimSeat(anyString(), anyInt(), any(LocalDate.class));
        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    @DisplayName("a full event is rejected without trying to claim a seat")
    void fullEventIsRejected() {
        existingStudent();
        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(openEvent(200, 200));

        assertThatThrownBy(() -> registrationService.register(STUDENT_ID, EVENT_ID))
                .isInstanceOf(EventFullException.class)
                .hasMessage("Event is full");

        verify(eventRepository, never()).claimSeat(anyString(), anyInt(), any(LocalDate.class));
    }

    @Test
    @DisplayName("losing the seat race reports a full event rather than overselling")
    void losingTheRaceReportsFull() {
        existingStudent();
        // The pre-read says there is room...
        Event event = openEvent(200, 199);
        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(event);
        when(registrationRepository.findActiveRegistration(STUDENT_ID, EVENT_ID)).thenReturn(Optional.empty());

        // ...but the atomic claim fails because another request took the last seat.
        when(eventRepository.claimSeat(eq(EVENT_ID), eq(200), any(LocalDate.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.register(STUDENT_ID, EVENT_ID))
                .isInstanceOf(EventFullException.class);

        verify(registrationRepository, never()).save(any(Registration.class));
    }

    @Test
    @DisplayName("an unapproved event cannot be registered for")
    void pendingEventIsNotOpenForRegistration() {
        existingStudent();
        Event pending = openEvent(200, 0);
        pending.setStatus(EventStatus.PENDING);
        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(pending);

        assertThatThrownBy(() -> registrationService.register(STUDENT_ID, EVENT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("not open for registration");
    }

    @Test
    @DisplayName("registration after the deadline is refused")
    void pastDeadlineIsRefused() {
        existingStudent();
        Event closed = openEvent(200, 0);
        closed.setRegistrationDeadline(LocalDate.now().minusDays(1));
        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(closed);

        assertThatThrownBy(() -> registrationService.register(STUDENT_ID, EVENT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Registration for this event has closed");
    }

    @Test
    @DisplayName("non-students cannot register")
    void organizersCannotRegister() {
        User organizer = student();
        organizer.setRole(Role.ORGANIZER);
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(organizer));

        assertThatThrownBy(() -> registrationService.register(STUDENT_ID, EVENT_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Only students can register");
    }

    @Test
    @DisplayName("a duplicate caught by the unique index releases the seat it had taken")
    void duplicateKeyReleasesTheClaimedSeat() {
        existingStudent();
        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(openEvent(200, 120));
        when(registrationRepository.findActiveRegistration(STUDENT_ID, EVENT_ID)).thenReturn(Optional.empty());
        when(eventRepository.claimSeat(eq(EVENT_ID), eq(200), any(LocalDate.class)))
                .thenReturn(Optional.of(openEvent(200, 121)));
        when(registrationRepository.save(any(Registration.class)))
                .thenThrow(new DuplicateKeyException("uniq_active_registration"));

        assertThatThrownBy(() -> registrationService.register(STUDENT_ID, EVENT_ID))
                .isInstanceOf(DuplicateRegistrationException.class);

        // The important part: the seat is handed back, not leaked.
        verify(eventRepository).releaseSeat(EVENT_ID);
    }

    // ------------------------------------------------------------------
    // Cancellation
    // ------------------------------------------------------------------

    @Test
    @DisplayName("cancelling marks the registration and releases the seat")
    void cancellationReleasesTheSeat() {
        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(openEvent(200, 121));

        Registration active = new Registration();
        active.setId("registration-1");
        active.setStatus(RegistrationStatus.CONFIRMED);
        when(registrationRepository.findFirstByUserIdAndEventIdOrderByRegisteredAtDesc(STUDENT_ID, EVENT_ID))
                .thenReturn(Optional.of(active));
        when(registrationRepository.cancelConfirmedRegistration(
                eq("registration-1"), eq(STUDENT_ID), eq(EVENT_ID), any(Instant.class)))
                .thenAnswer(invocation -> {
                    active.setStatus(RegistrationStatus.CANCELLED);
                    active.setCancelledAt(invocation.getArgument(3));
                    return Optional.of(active);
                });

        String message = registrationService.cancel(STUDENT_ID, EVENT_ID);

        assertThat(message).isEqualTo("Registration cancelled");
        assertThat(active.getStatus()).isEqualTo(RegistrationStatus.CANCELLED);
        assertThat(active.getCancelledAt()).isNotNull();
        verify(eventRepository).releaseSeat(EVENT_ID);
    }

    @Test
    @DisplayName("a concurrent cancellation releases no second seat")
    void concurrentCancellationDoesNotReleaseAnotherSeat() {
        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(openEvent(200, 0));
        Registration active = new Registration();
        active.setId("registration-1");
        active.setStatus(RegistrationStatus.CONFIRMED);
        Registration alreadyCancelled = new Registration();
        alreadyCancelled.setStatus(RegistrationStatus.CANCELLED);
        when(registrationRepository.findFirstByUserIdAndEventIdOrderByRegisteredAtDesc(STUDENT_ID, EVENT_ID))
                .thenReturn(Optional.of(active), Optional.of(alreadyCancelled));
        when(registrationRepository.cancelConfirmedRegistration(
                eq("registration-1"), eq(STUDENT_ID), eq(EVENT_ID), any(Instant.class)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.cancel(STUDENT_ID, EVENT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already cancelled");

        verify(eventRepository, never()).releaseSeat(anyString());
    }

    @Test
    @DisplayName("cancelling something that was never registered is a 404")
    void cancellingWithoutRegistration() {
        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(openEvent(200, 0));
        when(registrationRepository.findFirstByUserIdAndEventIdOrderByRegisteredAtDesc(STUDENT_ID, EVENT_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.cancel(STUDENT_ID, EVENT_ID))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(eventRepository, never()).releaseSeat(anyString());
    }

    @Test
    @DisplayName("cancelling twice is refused and does not release a second seat")
    void cancellingTwiceIsRefused() {
        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(openEvent(200, 0));

        Registration cancelled = new Registration();
        cancelled.setStatus(RegistrationStatus.CANCELLED);
        when(registrationRepository.findFirstByUserIdAndEventIdOrderByRegisteredAtDesc(STUDENT_ID, EVENT_ID))
                .thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> registrationService.cancel(STUDENT_ID, EVENT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already cancelled");

        verify(eventRepository, never()).releaseSeat(anyString());
    }

    @Test
    @DisplayName("attendance already recorded means the registration can no longer be cancelled")
    void cancellingAfterAttendanceIsRefused() {
        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(openEvent(200, 1));

        Registration attended = new Registration();
        attended.setStatus(RegistrationStatus.ATTENDED);
        when(registrationRepository.findFirstByUserIdAndEventIdOrderByRegisteredAtDesc(STUDENT_ID, EVENT_ID))
                .thenReturn(Optional.of(attended));

        assertThatThrownBy(() -> registrationService.cancel(STUDENT_ID, EVENT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Attendance has already been recorded");
    }

    // ------------------------------------------------------------------
    // Reporting and exports
    // ------------------------------------------------------------------

    @Test
    @DisplayName("my registrations joins each row to its event")
    void myRegistrationsJoinEvents() {
        Registration confirmed = new Registration();
        confirmed.setId("r1");
        confirmed.setEventId(EVENT_ID);
        confirmed.setUserId(STUDENT_ID);
        confirmed.setStatus(RegistrationStatus.CONFIRMED);
        confirmed.setRegisteredAt(Instant.now());

        when(registrationRepository.findByUserIdOrderByRegisteredAtDesc(STUDENT_ID))
                .thenReturn(List.of(confirmed));
        when(eventRepository.findByIdIn(List.of(EVENT_ID))).thenReturn(List.of(openEvent(200, 10)));

        List<RegistrationResponse> registrations = registrationService.myRegistrations(STUDENT_ID);

        assertThat(registrations).hasSize(1);
        assertThat(registrations.get(0).eventTitle()).isEqualTo("AI Workshop");
        assertThat(registrations.get(0).status()).isEqualTo(RegistrationStatus.CONFIRMED);
    }

    @Test
    @DisplayName("attendee search narrows the list by student name or id")
    void attendeeSearchFilters() {
        Registration confirmed = new Registration();
        confirmed.setId("r1");
        confirmed.setEventId(EVENT_ID);
        confirmed.setUserId(STUDENT_ID);
        confirmed.setStatus(RegistrationStatus.CONFIRMED);
        confirmed.setRegisteredAt(Instant.now());

        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(openEvent(200, 1));
        when(registrationRepository.findByEventIdOrderByRegisteredAtAsc(EVENT_ID)).thenReturn(List.of(confirmed));
        when(userRepository.findByIdIn(List.of(STUDENT_ID))).thenReturn(List.of(student()));
        when(attendanceRepository.findByEventIdOrderByCheckedInAtAsc(EVENT_ID)).thenReturn(List.of());

        var matching = registrationService.attendees(
                EVENT_ID, "organizer-1", Role.ORGANIZER, "arun",
                org.springframework.data.domain.PageRequest.of(0, 20));
        var missing = registrationService.attendees(
                EVENT_ID, "organizer-1", Role.ORGANIZER, "nobody",
                org.springframework.data.domain.PageRequest.of(0, 20));

        assertThat(matching.getTotalElements()).isEqualTo(1);
        assertThat(missing.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("analytics reports attendance as a percentage of registrations")
    void analyticsComputesAttendancePercentage() {
        Event event = openEvent(200, 180);
        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(event);
        when(registrationRepository.countByEventIdAndStatus(EVENT_ID, RegistrationStatus.CONFIRMED)).thenReturn(150L);
        when(registrationRepository.countByEventIdAndStatus(EVENT_ID, RegistrationStatus.WAITLISTED)).thenReturn(0L);
        when(registrationRepository.countByEventIdAndStatus(EVENT_ID, RegistrationStatus.CANCELLED)).thenReturn(10L);
        when(registrationRepository.countByEventIdAndStatus(EVENT_ID, RegistrationStatus.ATTENDED)).thenReturn(30L);
        when(attendanceRepository.countByEventId(EVENT_ID)).thenReturn(150L);

        EventAnalyticsResponse analytics =
                registrationService.analytics(EVENT_ID, "organizer-1", Role.ORGANIZER);

        assertThat(analytics.capacity()).isEqualTo(200);
        assertThat(analytics.registrations()).isEqualTo(180);
        assertThat(analytics.availableSeats()).isEqualTo(20);
        // 150 / 180 = 83.33%, the worked example from the implementation document.
        assertThat(analytics.attendancePercentage()).isEqualTo(83.33);
    }

    @Test
    @DisplayName("attendance percentage is zero rather than NaN when nobody registered")
    void analyticsHandlesZeroRegistrations() {
        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(openEvent(50, 0));
        when(attendanceRepository.countByEventId(EVENT_ID)).thenReturn(0L);

        EventAnalyticsResponse analytics =
                registrationService.analytics(EVENT_ID, "organizer-1", Role.ORGANIZER);

        assertThat(analytics.attendancePercentage()).isZero();
    }

    @Test
    @DisplayName("CSV export uses the documented columns and one row per attendee")
    void csvExportShape() {
        Registration first = new Registration();
        first.setId("r1");
        first.setEventId(EVENT_ID);
        first.setUserId(STUDENT_ID);
        first.setStatus(RegistrationStatus.CONFIRMED);
        first.setRegisteredAt(Instant.now());

        User other = student();
        other.setId("student-2");
        other.setName("Priya, Sharma");   // comma forces quoting
        other.setStudentId("CS002");
        other.setEmail("priya@example.com");
        other.setDepartment("IT");

        Registration second = new Registration();
        second.setId("r2");
        second.setEventId(EVENT_ID);
        second.setUserId("student-2");
        second.setStatus(RegistrationStatus.ATTENDED);
        second.setRegisteredAt(Instant.now());

        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(openEvent(200, 2));
        when(registrationRepository.findByEventIdOrderByRegisteredAtAsc(EVENT_ID))
                .thenReturn(List.of(first, second));
        when(userRepository.findByIdIn(any())).thenReturn(List.of(student(), other));
        when(attendanceRepository.findByEventIdOrderByCheckedInAtAsc(EVENT_ID)).thenReturn(List.of());

        String csv = registrationService.exportCsv(EVENT_ID, "organizer-1", Role.ORGANIZER);
        List<String> lines = csv.lines().toList();

        assertThat(lines.get(0)).isEqualTo("Student ID,Name,Email,Department,Status");
        assertThat(lines).hasSize(3);
        assertThat(lines.get(1)).isEqualTo("CS2026001,Arun Kumar,arun@example.com,CSE,CONFIRMED");
        assertThat(lines.get(2)).contains("\"Priya, Sharma\"");
        assertThat(lines.get(2)).endsWith(",ATTENDED");
    }

    @Test
    @DisplayName("JSON export returns the documented flat shape")
    void jsonExportShape() {
        Registration confirmed = new Registration();
        confirmed.setId("r1");
        confirmed.setEventId(EVENT_ID);
        confirmed.setUserId(STUDENT_ID);
        confirmed.setStatus(RegistrationStatus.CONFIRMED);
        confirmed.setRegisteredAt(Instant.now());

        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(openEvent(200, 1));
        when(registrationRepository.findByEventIdOrderByRegisteredAtAsc(EVENT_ID)).thenReturn(List.of(confirmed));
        when(userRepository.findByIdIn(any())).thenReturn(List.of(student()));
        when(attendanceRepository.findByEventIdOrderByCheckedInAtAsc(EVENT_ID)).thenReturn(List.of());

        var rows = registrationService.exportRows(EVENT_ID, "organizer-1", Role.ORGANIZER);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).studentId()).isEqualTo("CS2026001");
        assertThat(rows.get(0).name()).isEqualTo("Arun Kumar");
        assertThat(rows.get(0).email()).isEqualTo("arun@example.com");
        assertThat(rows.get(0).department()).isEqualTo("CSE");
        assertThat(rows.get(0).status()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("an organizer cannot read another organizer's attendee list")
    void attendeesRejectForeignOrganizer() {
        Event foreign = openEvent(200, 1);
        foreign.setOrganizerId("organizer-2");
        when(eventService.findEventOrThrow(EVENT_ID)).thenReturn(foreign);

        org.mockito.Mockito.doThrow(new ForbiddenException("You can only manage events that you created"))
                .when(eventService).requireOwnership(foreign, "organizer-1");

        assertThatThrownBy(() -> registrationService.attendees(
                EVENT_ID, "organizer-1", Role.ORGANIZER, null,
                org.springframework.data.domain.PageRequest.of(0, 20)))
                .isInstanceOf(ForbiddenException.class);
    }
}
