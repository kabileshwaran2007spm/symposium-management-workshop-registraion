package com.campusconnect.service;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.campusconnect.dto.EventFilter;
import com.campusconnect.dto.EventRequest;
import com.campusconnect.dto.EventResponse;
import com.campusconnect.exception.BusinessRuleException;
import com.campusconnect.exception.ForbiddenException;
import com.campusconnect.exception.ResourceNotFoundException;
import com.campusconnect.model.Event;
import com.campusconnect.model.EventStatus;
import com.campusconnect.model.RegistrationStatus;
import com.campusconnect.model.Role;
import com.campusconnect.model.User;
import com.campusconnect.repository.EventRepository;
import com.campusconnect.repository.RegistrationRepository;
import com.campusconnect.repository.UserRepository;

/**
 * Event lifecycle: creation, editing, deletion and discovery.
 *
 * <p>Two rules are enforced here rather than in controllers: an organizer may only
 * touch their own events (ownership), and status changes must follow the documented
 * state machine.
 */
@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    /** Statuses only the owner or an administrator may see. */
    private static final Set<EventStatus> NON_PUBLIC_STATUSES =
            Set.of(EventStatus.DRAFT, EventStatus.PENDING, EventStatus.REJECTED);

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final UserRepository userRepository;

    public EventService(
            EventRepository eventRepository,
            RegistrationRepository registrationRepository,
            UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.userRepository = userRepository;
    }

    // ------------------------------------------------------------------
    // Creation and editing
    // ------------------------------------------------------------------

    public EventResponse createEvent(EventRequest request, String organizerId) {
        Event event = new Event();
        applyRequest(event, request);

        // The application flow saves new events as PENDING by default; DRAFT is
        // opt-in via the "Save Draft" action. APPROVED can never come from a client.
        EventStatus requested = request.status();
        event.setStatus(requested == EventStatus.DRAFT ? EventStatus.DRAFT : EventStatus.PENDING);

        event.setRegisteredCount(0);
        event.setOrganizerId(organizerId);
        event.setCreatedAt(Instant.now());
        event.setUpdatedAt(Instant.now());

        Event saved = eventRepository.save(event);
        log.info("Organizer {} created event {} with status {}", organizerId, saved.getId(), saved.getStatus());

        return toResponse(saved, true);
    }

    public EventResponse updateEvent(String eventId, EventRequest request, String requesterId) {
        Event event = findEventOrThrow(eventId);
        requireOwnership(event, requesterId);

        if (event.getStatus() == EventStatus.CANCELLED || event.getStatus() == EventStatus.COMPLETED) {
            throw new BusinessRuleException(
                    "A " + event.getStatus().name().toLowerCase() + " event can no longer be edited");
        }

        if (request.capacity() < event.getRegisteredCount()) {
            throw new BusinessRuleException("Capacity cannot be lower than the "
                    + event.getRegisteredCount() + " students already registered");
        }

        EventStatus previousStatus = event.getStatus();
        applyRequest(event, request);

        // Editing a rejected or draft event may resubmit it for approval. Editing
        // an approved event keeps it approved, so an organizer is not forced to wait
        // for re-approval to fix a typo.
        EventStatus requested = request.status();
        if (requested != null && previousStatus != EventStatus.APPROVED) {
            if (previousStatus.canTransitionTo(requested)) {
                event.setStatus(requested);
            } else {
                throw new BusinessRuleException(
                        "Cannot move an event from " + previousStatus + " to " + requested);
            }
        }

        event.setUpdatedAt(Instant.now());
        Event saved = eventRepository.save(event);
        return toResponse(saved, true);
    }

    /**
     * Deletes an event only while it has no seat-holding registrations. Once
     * students hold seats, the event is cancelled instead so their registration
     * history and QR passes are not silently destroyed.
     *
     * @return a message describing which of the two outcomes happened.
     */
    public String deleteEvent(String eventId, String requesterId) {
        Event event = findEventOrThrow(eventId);
        requireOwnership(event, requesterId);

        long activeRegistrations = registrationRepository.countByEventIdAndStatus(
                        eventId, RegistrationStatus.CONFIRMED)
                + registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.ATTENDED);

        if (activeRegistrations > 0) {
            if (event.getStatus() != EventStatus.CANCELLED) {
                event.setStatus(EventStatus.CANCELLED);
                event.setUpdatedAt(Instant.now());
                eventRepository.save(event);
            }
            return "Event has " + activeRegistrations
                    + " active registration(s), so it was cancelled instead of deleted";
        }

        eventRepository.deleteById(eventId);
        return "Event deleted";
    }

    // ------------------------------------------------------------------
    // Reading
    // ------------------------------------------------------------------

    /** Public discovery: approved events only. */
    public Page<Event> searchPublicEvents(EventFilter filter, Pageable pageable) {
        filter.setAllowedStatuses(EventStatus.STUDENT_VISIBLE);
        return eventRepository.search(filter, pageable);
    }

    /** An organizer's own events, in any status. */
    public Page<Event> searchOrganizerEvents(String organizerId, EventFilter filter, Pageable pageable) {
        filter.setOrganizerId(organizerId);
        filter.setAllowedStatuses(null);
        return eventRepository.search(filter, pageable);
    }

    /** Administrative view across every status. */
    public Page<Event> searchAllEvents(EventFilter filter, Pageable pageable) {
        filter.setAllowedStatuses(null);
        return eventRepository.search(filter, pageable);
    }

    /**
     * Single event lookup honouring visibility.
     *
     * <p>An event that is not public reads as 404 to anyone except its organizer or
     * an administrator, so unpublished events are not even discoverable by id.
     */
    public Event getVisibleEvent(String eventId, String requesterId, Role requesterRole) {
        Event event = findEventOrThrow(eventId);

        boolean isOwner = requesterId != null && requesterId.equals(event.getOrganizerId());
        boolean isAdmin = requesterRole == Role.ADMIN;

        if (NON_PUBLIC_STATUSES.contains(event.getStatus()) && !isOwner && !isAdmin) {
            throw new ResourceNotFoundException("Event not found");
        }
        return event;
    }

    public Event findEventOrThrow(String eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    }

    public void requireOwnership(Event event, String requesterId) {
        if (!event.getOrganizerId().equals(requesterId)) {
            throw new ForbiddenException("You can only manage events that you created");
        }
    }

    // ------------------------------------------------------------------
    // Response mapping
    // ------------------------------------------------------------------

    public boolean isRegistrationOpen(Event event) {
        return event.getStatus() == EventStatus.APPROVED
                && !event.isFull()
                && !event.getRegistrationDeadline().isBefore(LocalDate.now())
                && !event.getDate().isBefore(LocalDate.now());
    }

    /** Maps one event, looking its organizer up. */
    public EventResponse toResponse(Event event, boolean includeRegistrationState) {
        return EventResponse.from(event, organizerName(event.getOrganizerId()),
                includeRegistrationState && isRegistrationOpen(event));
    }

    /**
     * Maps a page of events while resolving organizer names in a single extra query,
     * so listing N events does not issue N user lookups.
     */
    public Page<EventResponse> toResponsePage(Page<Event> events, boolean includeRegistrationState) {
        Map<String, String> names = organizerNames(events.getContent());
        return events.map(event -> EventResponse.from(
                event,
                names.get(event.getOrganizerId()),
                includeRegistrationState && isRegistrationOpen(event)));
    }

    public Map<String, String> organizerNames(Collection<Event> events) {
        Set<String> ids = events.stream()
                .map(Event::getOrganizerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));

        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<String, String> names = new HashMap<>();
        for (User organizer : userRepository.findByIdIn(ids)) {
            names.put(organizer.getId(), organizer.getName());
        }
        return names;
    }

    public String organizerName(String organizerId) {
        if (organizerId == null) {
            return null;
        }
        return userRepository.findById(organizerId).map(User::getName).orElse(null);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    /** Copies request fields onto the event, then validates the combined result. */
    private void applyRequest(Event event, EventRequest request) {
        event.setTitle(request.title().trim());
        event.setDescription(request.description().trim());
        event.setCategory(request.category());
        event.setDepartment(request.department().trim());
        event.setDate(request.date());
        event.setStartTime(request.startTime());
        event.setEndTime(request.endTime());
        event.setVenue(request.venue().trim());
        event.setCapacity(request.capacity());
        event.setRegistrationDeadline(request.registrationDeadline());

        validateSchedule(event);
    }

    /**
     * Cross-field rules. These cannot be expressed as field-level bean validation
     * because each one compares two fields, and keeping them here means the same
     * checks run for both create and update.
     */
    private void validateSchedule(Event event) {
        if (!event.getEndTime().isAfter(event.getStartTime())) {
            throw new BusinessRuleException("End time must be after the start time");
        }
        if (event.getRegistrationDeadline().isAfter(event.getDate())) {
            throw new BusinessRuleException("Registration deadline must be on or before the event date");
        }
        LocalDate today = LocalDate.now();
        if (event.getDate().isBefore(today)) {
            throw new BusinessRuleException("Event date cannot be in the past");
        }
        if (event.getRegistrationDeadline().isBefore(today)) {
            throw new BusinessRuleException("Registration deadline cannot be in the past");
        }
    }
}
