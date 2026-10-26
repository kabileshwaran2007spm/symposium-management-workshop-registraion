package com.campusconnect.service;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.campusconnect.dto.AdminAnalyticsResponse;
import com.campusconnect.dto.AdminRegistrationResponse;
import com.campusconnect.dto.ClubResponse;
import com.campusconnect.dto.EventFilter;
import com.campusconnect.dto.EventResponse;
import com.campusconnect.dto.PageResponse;
import com.campusconnect.dto.UserResponse;
import com.campusconnect.exception.BusinessRuleException;
import com.campusconnect.exception.ResourceNotFoundException;
import com.campusconnect.model.Event;
import com.campusconnect.model.EventStatus;
import com.campusconnect.model.Registration;
import com.campusconnect.model.RegistrationStatus;
import com.campusconnect.model.Role;
import com.campusconnect.model.User;
import com.campusconnect.repository.AttendanceRepository;
import com.campusconnect.repository.ClubRepository;
import com.campusconnect.repository.EventRepository;
import com.campusconnect.repository.RegistrationRepository;
import com.campusconnect.repository.UserRepository;

/**
 * Administrative operations: platform oversight and the event approval workflow.
 *
 * <p>Approval is the only place an event becomes visible to students, so the status
 * transition is validated strictly: anything other than PENDING cannot be approved
 * or rejected.
 */
@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final AttendanceRepository attendanceRepository;
    private final ClubRepository clubRepository;
    private final EventService eventService;

    public AdminService(
            UserRepository userRepository,
            EventRepository eventRepository,
            RegistrationRepository registrationRepository,
            AttendanceRepository attendanceRepository,
            ClubRepository clubRepository,
            EventService eventService) {
        this.userRepository = userRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.attendanceRepository = attendanceRepository;
        this.clubRepository = clubRepository;
        this.eventService = eventService;
    }

    // ------------------------------------------------------------------
    // Users
    // ------------------------------------------------------------------

    public Page<UserResponse> listUsers(Role role, Pageable pageable) {
        Page<User> users = role == null
                ? userRepository.findAll(pageable)
                : userRepository.findByRole(role, pageable);
        return users.map(UserResponse::from);
    }

    // ------------------------------------------------------------------
    // Events and approval
    // ------------------------------------------------------------------

    public Page<EventResponse> listEvents(EventFilter filter, Pageable pageable) {
        return eventService.toResponsePage(eventService.searchAllEvents(filter, pageable), false);
    }

    public EventResponse approve(String eventId) {
        Event event = findEventOrThrow(eventId);
        requireStatus(event, EventStatus.PENDING, "approved");
        return transition(event, EventStatus.APPROVED, "approved");
    }

    public EventResponse reject(String eventId) {
        Event event = findEventOrThrow(eventId);
        requireStatus(event, EventStatus.PENDING, "rejected");
        return transition(event, EventStatus.REJECTED, "rejected");
    }

    public Page<EventResponse> pendingEvents(Pageable pageable) {
        EventFilter filter = new EventFilter();
        filter.setStatus(EventStatus.PENDING);
        return eventService.toResponsePage(eventService.searchAllEvents(filter, pageable), false);
    }

    // ------------------------------------------------------------------
    // Registrations
    // ------------------------------------------------------------------

    public PageResponse<AdminRegistrationResponse> listRegistrations(
            String eventId, RegistrationStatus status, Pageable pageable) {

        Page<Registration> registrations;
        if (eventId != null && !eventId.isBlank()) {
            registrations = registrationRepository.findByEventId(eventId, pageable);
        } else {
            registrations = registrationRepository.findAll(pageable);
        }

        List<Registration> content = status == null
                ? registrations.getContent()
                : registrations.getContent().stream()
                        .filter(registration -> registration.getStatus() == status)
                        .toList();

        Map<String, Event> events = eventsById(content.stream().map(registration -> registration.getEventId()).distinct().toList());
        Map<String, User> users = usersById(content.stream().map(registration -> registration.getUserId()).distinct().toList());

        List<AdminRegistrationResponse> rows = content.stream()
                .map(registration -> AdminRegistrationResponse.from(
                        registration,
                        events.get(registration.getEventId()),
                        users.get(registration.getUserId())))
                .toList();

        Page<AdminRegistrationResponse> page = new org.springframework.data.domain.PageImpl<>(
                rows, pageable, registrations.getTotalElements());
        return PageResponse.from(page);
    }

    // ------------------------------------------------------------------
    // Clubs
    // ------------------------------------------------------------------

    public List<ClubResponse> listClubs() {
        return clubRepository.findAll().stream()
                .map(ClubResponse::from)
                .sorted(java.util.Comparator.comparing(
                        club -> club.name() == null ? null : club.name(),
                        java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    // ------------------------------------------------------------------
    // Analytics
    // ------------------------------------------------------------------

    public AdminAnalyticsResponse analytics() {
        Map<Role, Long> usersByRole = new EnumMap<>(Role.class);
        for (Role role : Role.values()) {
            usersByRole.put(role, userRepository.countByRole(role));
        }

        Map<EventStatus, Long> eventsByStatus = new EnumMap<>(EventStatus.class);
        long totalEvents = 0;
        for (EventStatus status : EventStatus.values()) {
            long count = eventRepository.countByStatus(status);
            eventsByStatus.put(status, count);
            totalEvents += count;
        }

        Map<RegistrationStatus, Long> registrationsByStatus = new EnumMap<>(RegistrationStatus.class);
        long totalRegistrations = 0;
        for (RegistrationStatus status : RegistrationStatus.values()) {
            long count = registrationRepository.countByStatus(status);
            registrationsByStatus.put(status, count);
            totalRegistrations += count;
        }

        return new AdminAnalyticsResponse(
                userRepository.count(),
                usersByRole,
                totalEvents,
                eventsByStatus.getOrDefault(EventStatus.PENDING, 0L),
                eventsByStatus,
                totalRegistrations,
                registrationsByStatus,
                clubRepository.count(),
                attendanceRepository.count());
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    private Event findEventOrThrow(String eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    }

    private void requireStatus(Event event, EventStatus expected, String action) {
        if (event.getStatus() != expected) {
            throw new BusinessRuleException("Only " + expected + " events can be " + action
                    + " (this event is " + event.getStatus() + ")");
        }
    }

    private EventResponse transition(Event event, EventStatus target, String verb) {
        if (!event.getStatus().canTransitionTo(target)) {
            throw new BusinessRuleException("Cannot move an event from " + event.getStatus() + " to " + target);
        }
        event.setStatus(target);
        event.setUpdatedAt(Instant.now());
        Event saved = eventRepository.save(event);

        log.info("Event {} {}", saved.getId(), verb);
        return EventResponse.from(saved, eventService.organizerName(saved.getOrganizerId()), false);
    }

    private Map<String, Event> eventsById(List<String> ids) {
        List<String> safeIds = ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .distinct()
                .toList();
        if (safeIds.isEmpty()) {
            return Map.of();
        }
        return eventRepository.findByIdIn(safeIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(event -> event.getId(), Function.identity(), (a, b) -> a));
    }

    private Map<String, User> usersById(List<String> ids) {
        List<String> safeIds = ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> !id.isBlank())
                .distinct()
                .toList();
        if (safeIds.isEmpty()) {
            return Map.of();
        }
        return userRepository.findByIdIn(safeIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(user -> user.getId(), Function.identity(), (a, b) -> a));
    }
}
