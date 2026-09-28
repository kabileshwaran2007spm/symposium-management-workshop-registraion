package com.campusconnect.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.campusconnect.dto.AttendeeResponse;
import com.campusconnect.dto.EventAnalyticsResponse;
import com.campusconnect.dto.RegistrationResponse;
import com.campusconnect.exception.BusinessRuleException;
import com.campusconnect.exception.DuplicateRegistrationException;
import com.campusconnect.exception.EventFullException;
import com.campusconnect.exception.ForbiddenException;
import com.campusconnect.exception.ResourceNotFoundException;
import com.campusconnect.model.Attendance;
import com.campusconnect.model.Event;
import com.campusconnect.model.EventStatus;
import com.campusconnect.model.Registration;
import com.campusconnect.model.RegistrationStatus;
import com.campusconnect.model.Role;
import com.campusconnect.model.User;
import com.campusconnect.repository.AttendanceRepository;
import com.campusconnect.repository.EventRepository;
import com.campusconnect.repository.RegistrationRepository;
import com.campusconnect.repository.UserRepository;
import com.campusconnect.util.CsvWriter;

/**
 * The registration rules: who may register, when, how many, and what happens when
 * they cancel.
 *
 * <p>The interesting part is how a seat is claimed. Registration is not
 * "read the count, compare, then increment" - that sequence lets two simultaneous
 * requests both see a free seat and oversell the event. Instead the capacity
 * predicate is pushed into an atomic {@code findAndModify}, and the unique index on
 * {@code (userId, eventId)} catches a duplicate registration that races past the
 * pre-check. If either guard fires after a seat was taken, the seat is given back.
 */
@Service
public class RegistrationService {

    private static final Logger log = LoggerFactory.getLogger(RegistrationService.class);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int QR_TOKEN_BYTES = 24;

    private static final List<String> EXPORT_HEADERS =
            List.of("Student ID", "Name", "Email", "Department", "Status");

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final EventService eventService;

    public RegistrationService(
            RegistrationRepository registrationRepository,
            EventRepository eventRepository,
            UserRepository userRepository,
            AttendanceRepository attendanceRepository,
            EventService eventService) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.attendanceRepository = attendanceRepository;
        this.eventService = eventService;
    }

    // ------------------------------------------------------------------
    // Registration
    // ------------------------------------------------------------------

    public RegistrationResponse register(String userId, String eventId) {
        User student = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User account not found"));

        if (student.getRole() != Role.STUDENT) {
            throw new ForbiddenException("Only students can register for events");
        }

        LocalDate today = LocalDate.now();
        Event event = eventService.findEventOrThrow(eventId);
        assertOpenForRegistration(event, today);

        if (event.isFull()) {
            throw new EventFullException();
        }

        if (registrationRepository.findActiveRegistration(userId, eventId).isPresent()) {
            throw new DuplicateRegistrationException();
        }

        // Atomic seat claim: the capacity check and the increment happen together.
        Event claimed = eventRepository.claimSeat(eventId, event.getCapacity(), today).orElse(null);
        if (claimed == null) {
            // Either the last seat went to someone else, or the event stopped
            // accepting registrations between our read and the claim. Re-read to
            // report which, instead of guessing.
            Event current = eventService.findEventOrThrow(eventId);
            assertOpenForRegistration(current, today);
            throw new EventFullException();
        }

        try {
            Registration registration = new Registration();
            registration.setUserId(userId);
            registration.setEventId(eventId);
            registration.setStatus(RegistrationStatus.CONFIRMED);
            registration.setRegisteredAt(Instant.now());
            registration.setQrToken(generateQrToken());

            Registration saved = registrationRepository.save(registration);
            log.info("Student {} registered for event {}", userId, eventId);
            return RegistrationResponse.from(saved, claimed);

        } catch (DuplicateKeyException ex) {
            // A concurrent request from the same student beat us to it: hand the
            // seat straight back rather than leaking it.
            eventRepository.releaseSeat(eventId);
            throw new DuplicateRegistrationException();
        } catch (RuntimeException ex) {
            eventRepository.releaseSeat(eventId);
            throw ex;
        }
    }

    public String cancel(String userId, String eventId) {
        eventService.findEventOrThrow(eventId);

        Registration registration = registrationRepository
                .findFirstByUserIdAndEventIdOrderByRegisteredAtDesc(userId, eventId)
                .orElseThrow(() -> new ResourceNotFoundException("You are not registered for this event"));

        if (registration.getStatus() == RegistrationStatus.CANCELLED) {
            throw new BusinessRuleException("This registration is already cancelled");
        }
        if (registration.getStatus() == RegistrationStatus.ATTENDED) {
            throw new BusinessRuleException(
                    "Attendance has already been recorded, so this registration cannot be cancelled");
        }

        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(Instant.now());
        registrationRepository.save(registration);

        // Frees the seat for the next student. The update is guarded so the count
        // can never go negative even if called twice.
        eventRepository.releaseSeat(eventId);

        log.info("Student {} cancelled registration for event {}", userId, eventId);
        return "Registration cancelled";
    }

    public List<RegistrationResponse> myRegistrations(String userId) {
        List<Registration> registrations =
                registrationRepository.findByUserIdOrderByRegisteredAtDesc(userId);

        if (registrations.isEmpty()) {
            return List.of();
        }

        Map<String, Event> events = eventsById(registrations.stream()
                .map(Registration::getEventId)
                .distinct()
                .toList());

        return registrations.stream()
                .map(registration -> {
                    Event event = events.get(registration.getEventId());
                    return event == null ? null : RegistrationResponse.from(registration, event);
                })
                .filter(Objects::nonNull)
                .toList();
    }

    // ------------------------------------------------------------------
    // Organizer views
    // ------------------------------------------------------------------

    /**
     * Attendee list for an event, optionally narrowed by a student search term.
     *
     * <p>Searching matches student name, student id or email, which live on the user
     * document rather than the registration, so the join happens in memory. Event
     * attendee lists are bounded by the event's capacity, which keeps this
     * straightforward approach appropriate; a cross-collection aggregation would be
     * the next step if attendee lists ever grew into the hundreds of thousands.
     */
    public Page<AttendeeResponse> attendees(
            String eventId, String requesterId, Role requesterRole, String search, Pageable pageable) {

        Event event = requireOrganizerAccess(eventId, requesterId, requesterRole);

        List<Registration> registrations = registrationRepository.findByEventIdOrderByRegisteredAtAsc(eventId);
        if (registrations.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, 0);
        }

        Map<String, User> users = usersById(registrations.stream()
                .map(Registration::getUserId)
                .distinct()
                .toList());

        Map<String, Attendance> attendanceByRegistration = attendanceFor(eventId).stream()
                .collect(Collectors.toMap(Attendance::getRegistrationId, Function.identity(), (a, b) -> a));

        String term = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);

        List<AttendeeResponse> rows = registrations.stream()
                .map(registration -> AttendeeResponse.from(
                        registration,
                        users.get(registration.getUserId()),
                        attendanceByRegistration.containsKey(registration.getId())))
                .filter(row -> term.isEmpty() || matches(row, term))
                .sorted(Comparator.comparing(AttendeeResponse::name,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        return paginate(rows, pageable);
    }

    public EventAnalyticsResponse analytics(String eventId, String requesterId, Role requesterRole) {
        Event event = requireOrganizerAccess(eventId, requesterId, requesterRole);

        return EventAnalyticsResponse.of(
                event.getId(),
                event.getTitle(),
                event.getCapacity(),
                event.getRegisteredCount(),
                event.getAvailableSeats(),
                registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CONFIRMED),
                registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.WAITLISTED),
                registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.CANCELLED),
                registrationRepository.countByEventIdAndStatus(eventId, RegistrationStatus.ATTENDED),
                attendanceRepository.countByEventId(eventId));
    }

    // ------------------------------------------------------------------
    // Exports
    // ------------------------------------------------------------------

    /**
     * Flat rows backing both exports, so the CSV and JSON endpoints cannot drift
     * apart. Cancelled registrations are included because the export is a record of
     * what happened, not just who is currently attending.
     */
    public List<AttendeeResponse.ExportRow> exportRows(String eventId, String requesterId, Role requesterRole) {
        requireOrganizerAccess(eventId, requesterId, requesterRole);

        return exportAttendees(eventId).stream()
                .map(AttendeeResponse::toExportRow)
                .toList();
    }

    public String exportCsv(String eventId, String requesterId, Role requesterRole) {
        List<AttendeeResponse.ExportRow> rows = exportRows(eventId, requesterId, requesterRole);

        List<List<String>> values = rows.stream()
                .map(row -> List.of(
                        nullToEmpty(row.studentId()),
                        nullToEmpty(row.name()),
                        nullToEmpty(row.email()),
                        nullToEmpty(row.department()),
                        nullToEmpty(row.status())))
                .toList();

        return CsvWriter.toCsv(EXPORT_HEADERS, values);
    }

    // ------------------------------------------------------------------
    // Internals
    // ------------------------------------------------------------------

    /** Validates ownership for a non-admin caller and returns the event. */
    private Event requireOrganizerAccess(String eventId, String requesterId, Role requesterRole) {
        Event event = eventService.findEventOrThrow(eventId);
        if (requesterRole != Role.ADMIN) {
            eventService.requireOwnership(event, requesterId);
        }
        return event;
    }

    private void assertOpenForRegistration(Event event, LocalDate today) {
        if (event.getStatus() != EventStatus.APPROVED) {
            throw new BusinessRuleException("This event is not open for registration");
        }
        if (event.getDate().isBefore(today)) {
            throw new BusinessRuleException("This event has already taken place");
        }
        if (event.getRegistrationDeadline().isBefore(today)) {
            throw new BusinessRuleException("Registration for this event has closed");
        }
    }

    private List<AttendeeResponse> exportAttendees(String eventId) {
        List<Registration> registrations = registrationRepository.findByEventIdOrderByRegisteredAtAsc(eventId);
        if (registrations.isEmpty()) {
            return List.of();
        }

        Map<String, User> users = usersById(registrations.stream()
                .map(Registration::getUserId)
                .distinct()
                .toList());

        Map<String, Attendance> attendanceByRegistration = attendanceFor(eventId).stream()
                .collect(Collectors.toMap(Attendance::getRegistrationId, Function.identity(), (a, b) -> a));

        return registrations.stream()
                .map(registration -> AttendeeResponse.from(
                        registration,
                        users.get(registration.getUserId()),
                        attendanceByRegistration.containsKey(registration.getId())))
                .toList();
    }

    private List<Attendance> attendanceFor(String eventId) {
        return attendanceRepository.findByEventIdOrderByCheckedInAtAsc(eventId);
    }

    private boolean matches(AttendeeResponse row, String term) {
        return contains(row.name(), term)
                || contains(row.studentId(), term)
                || contains(row.email(), term)
                || contains(row.department(), term);
    }

    private boolean contains(String value, String term) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(term);
    }

    /** Applies paging to an already-filtered list. */
    private <T> Page<T> paginate(List<T> items, Pageable pageable) {
        int start = (int) Math.min(pageable.getOffset(), items.size());
        int end = Math.min(start + pageable.getPageSize(), items.size());
        return new PageImpl<>(items.subList(start, end), pageable, items.size());
    }

    private Map<String, Event> eventsById(Collection<String> ids) {
        return eventRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(Event::getId, Function.identity(), (a, b) -> a));
    }

    private Map<String, User> usersById(Collection<String> ids) {
        return userRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
    }

    /**
     * The QR pass carries this token and nothing else, so a photographed pass
     * reveals no personal data. 192 bits of {@link SecureRandom} output makes
     * guessing a pass impractical.
     */
    private String generateQrToken() {
        byte[] bytes = new byte[QR_TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
