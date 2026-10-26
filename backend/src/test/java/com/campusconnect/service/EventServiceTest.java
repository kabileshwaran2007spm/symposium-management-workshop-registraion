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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import com.campusconnect.dto.EventFilter;
import com.campusconnect.dto.EventRequest;
import com.campusconnect.dto.EventResponse;
import com.campusconnect.exception.BusinessRuleException;
import com.campusconnect.exception.ForbiddenException;
import com.campusconnect.exception.ResourceNotFoundException;
import com.campusconnect.model.Event;
import com.campusconnect.model.EventCategory;
import com.campusconnect.model.EventStatus;
import com.campusconnect.model.RegistrationStatus;
import com.campusconnect.model.Role;
import com.campusconnect.model.User;
import com.campusconnect.repository.EventRepository;
import com.campusconnect.repository.RegistrationRepository;
import com.campusconnect.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EventServiceTest {

    private static final String ORGANIZER_ID = "organizer-1";
    private static final String EVENT_ID = "event-1";

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EventService eventService;

    private EventRequest request(EventStatus status) {
        return new EventRequest(
                "AI and Machine Learning Workshop",
                "An introductory hands-on workshop on machine learning concepts.",
                EventCategory.WORKSHOP,
                "CSE",
                LocalDate.now().plusDays(10),
                LocalTime.of(10, 0),
                LocalTime.of(13, 0),
                "Seminar Hall",
                200,
                LocalDate.now().plusDays(5),
                status);
    }

    private Event savedEvent(EventStatus status, int registeredCount) {
        Event event = new Event();
        event.setId(EVENT_ID);
        event.setTitle("AI and Machine Learning Workshop");
        event.setDescription("An introductory hands-on workshop on machine learning concepts.");
        event.setCategory(EventCategory.WORKSHOP);
        event.setDepartment("CSE");
        event.setDate(LocalDate.now().plusDays(10));
        event.setStartTime(LocalTime.of(10, 0));
        event.setEndTime(LocalTime.of(13, 0));
        event.setVenue("Seminar Hall");
        event.setCapacity(200);
        event.setRegisteredCount(registeredCount);
        event.setRegistrationDeadline(LocalDate.now().plusDays(5));
        event.setStatus(status);
        event.setOrganizerId(ORGANIZER_ID);
        return event;
    }

    private void savingAssignsId() {
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event event = invocation.getArgument(0);
            if (event.getId() == null) {
                event.setId(EVENT_ID);
            }
            return event;
        });
    }

    // ------------------------------------------------------------------
    // Creation
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a new event defaults to PENDING so an admin must approve it")
    void createDefaultsToPending() {
        savingAssignsId();

        EventResponse response = eventService.createEvent(request(null), ORGANIZER_ID);

        assertThat(response.status()).isEqualTo(EventStatus.PENDING);
        assertThat(response.registeredCount()).isZero();
        assertThat(response.availableSeats()).isEqualTo(200);
        assertThat(response.registrationOpen()).isFalse();
        assertThat(response.organizerId()).isEqualTo(ORGANIZER_ID);
    }

    @Test
    @DisplayName("save-draft creates a DRAFT event")
    void createAsDraft() {
        savingAssignsId();

        EventResponse response = eventService.createEvent(request(EventStatus.DRAFT), ORGANIZER_ID);

        assertThat(response.status()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    @DisplayName("a client asking for APPROVED gets PENDING instead, so approval cannot be self-granted")
    void createCannotSelfApprove() {
        savingAssignsId();

        EventResponse response = eventService.createEvent(request(EventStatus.APPROVED), ORGANIZER_ID);

        assertThat(response.status()).isEqualTo(EventStatus.PENDING);
    }

    @Test
    @DisplayName("end time must be after start time")
    void endTimeMustFollowStartTime() {
        EventRequest invalid = new EventRequest(
                "AI Workshop", "A description long enough to pass validation.",
                EventCategory.WORKSHOP, "CSE", LocalDate.now().plusDays(10),
                LocalTime.of(14, 0), LocalTime.of(12, 0), "Seminar Hall", 100,
                LocalDate.now().plusDays(5), null);

        assertThatThrownBy(() -> eventService.createEvent(invalid, ORGANIZER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("End time must be after the start time");
    }

    @Test
    @DisplayName("the registration deadline cannot fall after the event")
    void deadlineCannotFollowEventDate() {
        EventRequest invalid = new EventRequest(
                "AI Workshop", "A description long enough to pass validation.",
                EventCategory.WORKSHOP, "CSE", LocalDate.now().plusDays(10),
                LocalTime.of(10, 0), LocalTime.of(12, 0), "Seminar Hall", 100,
                LocalDate.now().plusDays(11), null);

        assertThatThrownBy(() -> eventService.createEvent(invalid, ORGANIZER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("deadline must be on or before the event date");
    }

    @Test
    @DisplayName("events cannot be created in the past")
    void eventCannotBeInThePast() {
        EventRequest invalid = new EventRequest(
                "AI Workshop", "A description long enough to pass validation.",
                EventCategory.WORKSHOP, "CSE", LocalDate.now().minusDays(1),
                LocalTime.of(10, 0), LocalTime.of(12, 0), "Seminar Hall", 100,
                LocalDate.now().minusDays(2), null);

        assertThatThrownBy(() -> eventService.createEvent(invalid, ORGANIZER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Event date cannot be in the past");
    }

    // ------------------------------------------------------------------
    // Editing
    // ------------------------------------------------------------------

    @Test
    @DisplayName("an organizer cannot edit an event they do not own")
    void updateRejectsForeignOrganizer() {
        Event existing = savedEvent(EventStatus.APPROVED, 0);
        existing.setOrganizerId("organizer-2");
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> eventService.updateEvent(EVENT_ID, request(null), ORGANIZER_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("only manage events that you created");

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    @DisplayName("capacity cannot be dropped below the number already registered")
    void capacityCannotDropBelowRegistrations() {
        Event existing = savedEvent(EventStatus.APPROVED, 150);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(existing));

        EventRequest shrink = new EventRequest(
                "AI and Machine Learning Workshop",
                "An introductory hands-on workshop on machine learning concepts.",
                EventCategory.WORKSHOP, "CSE", LocalDate.now().plusDays(10),
                LocalTime.of(10, 0), LocalTime.of(13, 0), "Seminar Hall", 100,
                LocalDate.now().plusDays(5), null);

        assertThatThrownBy(() -> eventService.updateEvent(EVENT_ID, shrink, ORGANIZER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Capacity cannot be lower than");
    }

    @Test
    @DisplayName("a cancelled event can no longer be edited")
    void cancelledEventIsNotEditable() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(savedEvent(EventStatus.CANCELLED, 0)));

        assertThatThrownBy(() -> eventService.updateEvent(EVENT_ID, request(null), ORGANIZER_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("can no longer be edited");
    }

    @Test
    @DisplayName("a rejected event can be edited and resubmitted for approval")
    void rejectedEventCanBeResubmitted() {
        Event rejected = savedEvent(EventStatus.REJECTED, 0);
        rejected.setOrganizerId(ORGANIZER_ID);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(rejected));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        EventResponse response = eventService.updateEvent(EVENT_ID, request(EventStatus.PENDING), ORGANIZER_ID);

        assertThat(response.status()).isEqualTo(EventStatus.PENDING);
    }

    @Test
    @DisplayName("editing an approved event does not knock it back for re-approval")
    void editingApprovedEventKeepsApproval() {
        Event approved = savedEvent(EventStatus.APPROVED, 10);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(approved));
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        EventResponse response = eventService.updateEvent(EVENT_ID, request(null), ORGANIZER_ID);

        assertThat(response.status()).isEqualTo(EventStatus.APPROVED);
    }

    // ------------------------------------------------------------------
    // Deletion
    // ------------------------------------------------------------------

    @Test
    @DisplayName("an event with no registrations is deleted outright")
    void deleteRemovesUnusedEvent() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(savedEvent(EventStatus.DRAFT, 0)));
        when(registrationRepository.countByEventIdAndStatus(anyString(), any(RegistrationStatus.class)))
                .thenReturn(0L);

        String message = eventService.deleteEvent(EVENT_ID, ORGANIZER_ID);

        assertThat(message).isEqualTo("Event deleted");
        verify(eventRepository).deleteById(EVENT_ID);
    }

    @Test
    @DisplayName("an event with active registrations is cancelled instead of deleted")
    void deleteCancelsEventWithRegistrations() {
        Event approved = savedEvent(EventStatus.APPROVED, 5);
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(approved));
        when(registrationRepository.countByEventIdAndStatus(EVENT_ID, RegistrationStatus.CONFIRMED))
                .thenReturn(5L);
        when(registrationRepository.countByEventIdAndStatus(EVENT_ID, RegistrationStatus.ATTENDED))
                .thenReturn(0L);
        when(eventRepository.save(any(Event.class))).thenAnswer(i -> i.getArgument(0));

        String message = eventService.deleteEvent(EVENT_ID, ORGANIZER_ID);

        assertThat(message).contains("cancelled instead of deleted");
        verify(eventRepository, never()).deleteById(anyString());

        ArgumentCaptor<Event> saved = ArgumentCaptor.forClass(Event.class);
        verify(eventRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(EventStatus.CANCELLED);
    }

    // ------------------------------------------------------------------
    // Visibility and discovery
    // ------------------------------------------------------------------

    @Test
    @DisplayName("an unapproved event reads as 404 to anyone but its owner or an admin")
    void draftEventIsInvisibleToOthers() {
        when(eventRepository.findById(EVENT_ID)).thenReturn(Optional.of(savedEvent(EventStatus.DRAFT, 0)));

        assertThatThrownBy(() -> eventService.getVisibleEvent(EVENT_ID, "someone-else", Role.STUDENT))
                .isInstanceOf(ResourceNotFoundException.class);

        // The owner and an administrator can still see it.
        assertThat(eventService.getVisibleEvent(EVENT_ID, ORGANIZER_ID, Role.ORGANIZER)).isNotNull();
        assertThat(eventService.getVisibleEvent(EVENT_ID, "admin-1", Role.ADMIN)).isNotNull();
    }

    @Test
    @DisplayName("public discovery is limited to approved events")
    void publicSearchIsLimitedToApproved() {
        when(eventRepository.search(any(EventFilter.class), any())).thenReturn(Page.empty());

        EventFilter filter = new EventFilter();
        eventService.searchPublicEvents(filter, PageRequest.of(0, 10));

        ArgumentCaptor<EventFilter> captured = ArgumentCaptor.forClass(EventFilter.class);
        verify(eventRepository).search(captured.capture(), any());
        assertThat(captured.getValue().getAllowedStatuses()).containsExactly(EventStatus.APPROVED);
    }

    @Test
    @DisplayName("an organizer's own listing carries no status ceiling but is scoped to them")
    void organizerSearchIsScopedToTheOwner() {
        when(eventRepository.search(any(EventFilter.class), any())).thenReturn(Page.empty());

        EventFilter filter = new EventFilter();
        eventService.searchOrganizerEvents(ORGANIZER_ID, filter, PageRequest.of(0, 10));

        ArgumentCaptor<EventFilter> captured = ArgumentCaptor.forClass(EventFilter.class);
        verify(eventRepository).search(captured.capture(), any());
        assertThat(captured.getValue().getOrganizerId()).isEqualTo(ORGANIZER_ID);
        assertThat(captured.getValue().getAllowedStatuses()).isNull();
    }

    @Test
    @DisplayName("seat availability and the almost-full warning are derived, not stored")
    void seatAvailabilityIsDerived() {
        Event event = savedEvent(EventStatus.APPROVED, 120);
        assertThat(event.getAvailableSeats()).isEqualTo(80);
        assertThat(event.isFull()).isFalse();
        assertThat(event.isAlmostFull()).isFalse();

        event.setRegisteredCount(190);   // 10 left of 200 = exactly the 10% threshold
        assertThat(event.getAvailableSeats()).isEqualTo(10);
        assertThat(event.isAlmostFull()).isTrue();

        event.setRegisteredCount(200);
        assertThat(event.getAvailableSeats()).isZero();
        assertThat(event.isFull()).isTrue();
        assertThat(event.isAlmostFull()).isFalse();

        // Never negative, even if the count somehow overshoots.
        event.setRegisteredCount(205);
        assertThat(event.getAvailableSeats()).isZero();
    }

    @Test
    @DisplayName("a page of events resolves organizer names in one extra query")
    void organizerNamesAreResolvedInOneQuery() {
        Event first = savedEvent(EventStatus.APPROVED, 0);
        Event second = savedEvent(EventStatus.APPROVED, 0);
        second.setId("event-2");
        second.setOrganizerId("organizer-2");

        User organizer = new User();
        organizer.setId(ORGANIZER_ID);
        organizer.setName("Computer Science Club");
        User other = new User();
        other.setId("organizer-2");
        other.setName("Robotics Club");

        when(userRepository.findByIdIn(any())).thenReturn(List.of(organizer, other));

        Page<EventResponse> page = eventService.toResponsePage(
                new PageImpl<>(List.of(first, second), PageRequest.of(0, 10), 2), false);

        assertThat(page.getContent()).hasSize(2);
        assertThat(page.getContent().get(0).organizerName()).isEqualTo("Computer Science Club");
        assertThat(page.getContent().get(1).organizerName()).isEqualTo("Robotics Club");
    }

    @Test
    @DisplayName("registrationOpen combines status, deadline and capacity")
    void registrationOpenReflectsAllConditions() {
        Event open = savedEvent(EventStatus.APPROVED, 0);
        assertThat(eventService.isRegistrationOpen(open)).isTrue();

        Event closed = savedEvent(EventStatus.APPROVED, 0);
        closed.setRegistrationDeadline(LocalDate.now().minusDays(1));
        assertThat(eventService.isRegistrationOpen(closed)).isFalse();

        Event full = savedEvent(EventStatus.APPROVED, 200);
        assertThat(eventService.isRegistrationOpen(full)).isFalse();

        Event pending = savedEvent(EventStatus.PENDING, 0);
        assertThat(eventService.isRegistrationOpen(pending)).isFalse();
    }

    @Test
    @DisplayName("the status machine refuses illegal transitions")
    void statusMachineRefusesIllegalTransitions() {
        assertThat(EventStatus.PENDING.canTransitionTo(EventStatus.APPROVED)).isTrue();
        assertThat(EventStatus.PENDING.canTransitionTo(EventStatus.REJECTED)).isTrue();
        assertThat(EventStatus.APPROVED.canTransitionTo(EventStatus.CANCELLED)).isTrue();
        // Terminal states stay terminal.
        assertThat(EventStatus.CANCELLED.canTransitionTo(EventStatus.APPROVED)).isFalse();
        assertThat(EventStatus.COMPLETED.canTransitionTo(EventStatus.PENDING)).isFalse();
        // An approved event cannot silently return to the pending queue via the
        // generic transition, which keeps re-approval an explicit admin action.
        assertThat(EventStatus.APPROVED.canTransitionTo(EventStatus.PENDING)).isFalse();
        assertThat(EventStatus.DRAFT.canTransitionTo(EventStatus.APPROVED)).isFalse();
    }

    @Test
    @DisplayName("search filters reach the repository alongside the status ceiling")
    void searchFiltersAreForwarded() {
        when(eventRepository.search(any(EventFilter.class), any())).thenReturn(Page.empty());

        EventFilter filter = new EventFilter();
        filter.setSearch("machine");
        filter.setDepartment("CSE");
        filter.setCategory(EventCategory.WORKSHOP);

        eventService.searchPublicEvents(filter, PageRequest.of(0, 10));

        ArgumentCaptor<EventFilter> captured = ArgumentCaptor.forClass(EventFilter.class);
        verify(eventRepository).search(captured.capture(), any());

        EventFilter forwarded = captured.getValue();
        assertThat(forwarded.getSearch()).isEqualTo("machine");
        assertThat(forwarded.getDepartment()).isEqualTo("CSE");
        assertThat(forwarded.getCategory()).isEqualTo(EventCategory.WORKSHOP);
        assertThat(forwarded.getAllowedStatuses()).containsExactly(EventStatus.APPROVED);
    }
}
