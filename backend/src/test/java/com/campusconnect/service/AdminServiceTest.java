package com.campusconnect.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

import com.campusconnect.dto.AdminAnalyticsResponse;
import com.campusconnect.dto.ClubResponse;
import com.campusconnect.dto.EventResponse;
import com.campusconnect.exception.BusinessRuleException;
import com.campusconnect.exception.ResourceNotFoundException;
import com.campusconnect.model.Club;
import com.campusconnect.model.Event;
import com.campusconnect.model.EventCategory;
import com.campusconnect.model.EventStatus;
import com.campusconnect.model.RegistrationStatus;
import com.campusconnect.model.Role;
import com.campusconnect.repository.AttendanceRepository;
import com.campusconnect.repository.ClubRepository;
import com.campusconnect.repository.EventRepository;
import com.campusconnect.repository.RegistrationRepository;
import com.campusconnect.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminServiceTest {

    private static final String EVENT_ID = "event-1";

    @Mock
    private UserRepository userRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private EventService eventService;

    private AdminService adminService;

    @BeforeEach
    void setUp() {
        adminService = new AdminService(userRepository, eventRepository, registrationRepository,
                attendanceRepository, clubRepository, eventService);
    }

    private Event event(EventStatus status) {
        Event event = new Event();
        event.setId(EVENT_ID);
        event.setTitle("AI Workshop");
        event.setCategory(EventCategory.WORKSHOP);
        event.setDepartment("CSE");
        event.setDate(LocalDate.now().plusDays(10));
        event.setStartTime(LocalTime.of(10, 0));
        event.setEndTime(LocalTime.of(13, 0));
        event.setVenue("Seminar Hall");
        event.setCapacity(200);
        event.setRegisteredCount(0);
        event.setRegistrationDeadline(LocalDate.now().plusDays(5));
        event.setStatus(status);
        event.setOrganizerId("organizer-1");
        return event;
    }

    @Test
    @DisplayName("approving a pending event publishes it to students")
    void approvePendingEvent() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(EventStatus.PENDING)));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        EventResponse response = adminService.approve(EVENT_ID);

        assertThat(response.status()).isEqualTo(EventStatus.APPROVED);

        ArgumentCaptor<Event> saved = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(saved.capture());
        assertThat(saved.getValue().getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("rejecting a pending event marks it rejected")
    void rejectPendingEvent() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(EventStatus.PENDING)));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        EventResponse response = adminService.reject(EVENT_ID);

        assertThat(response.status()).isEqualTo(EventStatus.REJECTED);
    }

    @Test
    @DisplayName("only pending events can be approved, so an admin cannot bypass the queue")
    void cannotApproveAnAlreadyApprovedEvent() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(EventStatus.APPROVED)));

        assertThatThrownBy(() -> adminService.approve(EVENT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Only PENDING events can be approved");

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("a draft cannot be approved while it is still a draft")
    void cannotApproveDraft() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(EventStatus.DRAFT)));

        assertThatThrownBy(() -> adminService.approve(EVENT_ID))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("a cancelled event cannot be rejected back into circulation")
    void cannotRejectCancelledEvent() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(event(EventStatus.CANCELLED)));

        assertThatThrownBy(() -> adminService.reject(EVENT_ID))
                .isInstanceOf(BusinessRuleException.class);

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("approving an unknown event is a 404")
    void approveUnknownEvent() {
        when(eventRepository.findById(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminService.approve("missing"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Event not found");
    }

    @Test
    @DisplayName("analytics totals users, events and registrations and counts the pending queue")
    void analyticsAggregates() {
        when(userRepository.count()).thenReturn(2_450L);
        when(userRepository.countByRole(Role.STUDENT)).thenReturn(2_400L);
        when(userRepository.countByRole(Role.ORGANIZER)).thenReturn(45L);
        when(userRepository.countByRole(Role.ADMIN)).thenReturn(5L);

        when(eventRepository.countByStatus(EventStatus.PENDING)).thenReturn(8L);
        when(eventRepository.countByStatus(EventStatus.APPROVED)).thenReturn(120L);
        when(eventRepository.countByStatus(EventStatus.DRAFT)).thenReturn(0L);
        when(eventRepository.countByStatus(EventStatus.REJECTED)).thenReturn(0L);
        when(eventRepository.countByStatus(EventStatus.CANCELLED)).thenReturn(0L);
        when(eventRepository.countByStatus(EventStatus.COMPLETED)).thenReturn(0L);

        when(registrationRepository.countByStatus(RegistrationStatus.CONFIRMED)).thenReturn(8_000L);
        when(registrationRepository.countByStatus(RegistrationStatus.CANCELLED)).thenReturn(400L);
        when(registrationRepository.countByStatus(RegistrationStatus.WAITLISTED)).thenReturn(0L);
        when(registrationRepository.countByStatus(RegistrationStatus.ATTENDED)).thenReturn(20L);

        when(clubRepository.count()).thenReturn(3L);
        when(attendanceRepository.count()).thenReturn(20L);

        AdminAnalyticsResponse analytics = adminService.analytics();

        assertThat(analytics.totalUsers()).isEqualTo(2_450L);
        assertThat(analytics.totalEvents()).isEqualTo(128L);
        assertThat(analytics.pendingEvents()).isEqualTo(8L);
        assertThat(analytics.totalRegistrations()).isEqualTo(8_420L);
        assertThat(analytics.totalClubs()).isEqualTo(3L);
        assertThat(analytics.usersByRole()).containsEntry(Role.ADMIN, 5L);
        assertThat(analytics.eventsByStatus()).containsEntry(EventStatus.PENDING, 8L);
    }

    @Test
    @DisplayName("clubs are listed alphabetically")
    void clubsAreSortedByName() {
        Club robotics = new Club();
        robotics.setId("c2");
        robotics.setName("Robotics Club");
        robotics.setDepartment("ECE");
        robotics.setCreatedAt(Instant.now());

        Club computer = new Club();
        computer.setId("c1");
        computer.setName("Computer Science Club");
        computer.setDepartment("CSE");
        computer.setCreatedAt(Instant.now());

        when(clubRepository.findAll()).thenReturn(List.of(robotics, computer));

        List<ClubResponse> clubs = adminService.listClubs();

        assertThat(clubs).extracting(ClubResponse::name)
                .containsExactly("Computer Science Club", "Robotics Club");
    }
}
