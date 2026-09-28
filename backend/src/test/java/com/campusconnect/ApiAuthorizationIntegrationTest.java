package com.campusconnect;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.campusconnect.model.Event;
import com.campusconnect.model.EventCategory;
import com.campusconnect.model.EventStatus;
import com.campusconnect.model.Role;
import com.campusconnect.model.User;
import com.campusconnect.repository.AttendanceRepository;
import com.campusconnect.repository.ClubRepository;
import com.campusconnect.repository.EventRepository;
import com.campusconnect.repository.RegistrationRepository;
import com.campusconnect.repository.UserRepository;
import com.campusconnect.security.JwtService;

/**
 * Authorization across the real HTTP stack.
 *
 * <p>Unit tests prove the services refuse the wrong caller; this class proves the
 * <em>filter chain and path rules</em> do, which is what an attacker actually meets.
 * It runs against the local MongoDB using the {@code test} profile, so it never
 * touches development data.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private RegistrationRepository registrationRepository;

    @Autowired
    private AttendanceRepository attendanceRepository;

    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    private String adminToken;
    private String organizerToken;
    private String otherOrganizerToken;
    private String studentToken;
    private String eventId;

    @BeforeEach
    void setUp() {
        attendanceRepository.deleteAll();
        registrationRepository.deleteAll();
        eventRepository.deleteAll();
        clubRepository.deleteAll();
        userRepository.deleteAll();

        User admin = user("Admin", "admin@test.edu", "ADM001", Role.ADMIN);
        User organizer = user("CSE Club", "cse@test.edu", "FAC001", Role.ORGANIZER);
        User other = user("Robotics Club", "robotics@test.edu", "FAC002", Role.ORGANIZER);
        User student = user("Arun Kumar", "arun@test.edu", "CS2026001", Role.STUDENT);

        adminToken = jwtService.generateToken(admin);
        organizerToken = jwtService.generateToken(organizer);
        otherOrganizerToken = jwtService.generateToken(other);
        studentToken = jwtService.generateToken(student);

        Event event = new Event();
        event.setTitle("AI and Machine Learning Workshop");
        event.setDescription("Hands-on introduction to machine learning concepts.");
        event.setCategory(EventCategory.WORKSHOP);
        event.setDepartment("CSE");
        event.setDate(LocalDate.now().plusDays(10));
        event.setStartTime(LocalTime.of(10, 0));
        event.setEndTime(LocalTime.of(13, 0));
        event.setVenue("Seminar Hall");
        event.setCapacity(200);
        event.setRegisteredCount(0);
        event.setRegistrationDeadline(LocalDate.now().plusDays(5));
        event.setStatus(EventStatus.APPROVED);
        event.setOrganizerId(organizer.getId());
        event.setCreatedAt(Instant.now());
        event.setUpdatedAt(Instant.now());
        eventId = eventRepository.save(event).getId();
    }

    private User user(String name, String email, String studentId, Role role) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setStudentId(studentId);
        user.setDepartment("CSE");
        user.setRole(role);
        user.setPassword(passwordEncoder.encode("Password@123"));
        user.setCreatedAt(Instant.now());
        return userRepository.save(user);
    }

    private String validEventPayload() {
        return """
                {
                  "title": "Robotics Seminar",
                  "description": "A seminar about robotics and embedded systems.",
                  "category": "SEMINAR",
                  "department": "ECE",
                  "date": "%s",
                  "startTime": "14:00",
                  "endTime": "16:00",
                  "venue": "Auditorium",
                  "capacity": 100,
                  "registrationDeadline": "%s"
                }
                """.formatted(LocalDate.now().plusDays(20), LocalDate.now().plusDays(15));
    }

    // ------------------------------------------------------------------
    // Authentication
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a protected endpoint without a token is 401")
    void anonymousRequestIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("a malformed token is treated as unauthenticated")
    void malformedTokenIsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("public event discovery works with no token at all")
    void publicDiscoveryNeedsNoToken() throws Exception {
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].title").value("AI and Machine Learning Workshop"));
    }

    // ------------------------------------------------------------------
    // Role authorization
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a student is forbidden from admin endpoints")
    void studentCannotReachAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("an organizer is forbidden from admin endpoints")
    void organizerCannotReachAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("an admin can reach admin endpoints and never sees a password hash")
    void adminCanReachAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(4))
                .andExpect(jsonPath("$.data.content[0].password").doesNotExist());
    }

    @Test
    @DisplayName("a student cannot use the organizer workspace")
    void studentCannotReachOrganizerWorkspace() throws Exception {
        mockMvc.perform(get("/api/organizer/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("a student cannot create events, and is refused before the body is even parsed")
    void studentCannotCreateEvents() throws Exception {
        // A valid body proves the 403 comes from authorization, not validation.
        mockMvc.perform(post("/api/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEventPayload()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("an admin cannot create events: that stays an organizer responsibility")
    void adminCannotCreateEvents() throws Exception {
        mockMvc.perform(post("/api/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEventPayload()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("an organizer creates an event and it enters the pending queue")
    void organizerCanCreateEvent() throws Exception {
        mockMvc.perform(post("/api/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validEventPayload()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("an admin approves a pending event, publishing it to students")
    void adminApprovesPendingEvent() throws Exception {
        String createdId = eventRepository.findAll().stream()
                .filter(e -> e.getStatus() == EventStatus.PENDING)
                .findFirst()
                .map(Event::getId)
                .orElseGet(() -> {
                    Event pending = eventRepository.findById(eventId).orElseThrow();
                    pending.setStatus(EventStatus.PENDING);
                    return eventRepository.save(pending).getId();
                });

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/admin/events/" + createdId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    // ------------------------------------------------------------------
    // Ownership
    // ------------------------------------------------------------------

    @Test
    @DisplayName("an organizer cannot read another organizer's attendees")
    void foreignOrganizerCannotReadAttendees() throws Exception {
        mockMvc.perform(get("/api/organizer/events/" + eventId + "/attendees")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + otherOrganizerToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/organizer/events/" + eventId + "/attendees")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // Student registration through the HTTP stack
    // ------------------------------------------------------------------

    @Test
    @DisplayName("the student registration lifecycle behaves over HTTP")
    void studentRegistrationLifecycle() throws Exception {
        mockMvc.perform(post("/api/events/" + eventId + "/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.qrToken").isNotEmpty());

        // A second attempt is a 409, and the seat count is unchanged.
        mockMvc.perform(post("/api/events/" + eventId + "/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You are already registered for this event"));

        mockMvc.perform(get("/api/events/" + eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.registeredCount").value(1))
                .andExpect(jsonPath("$.data.availableSeats").value(199));

        mockMvc.perform(get("/api/users/me/registrations")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/events/" + eventId + "/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/events/" + eventId))
                .andExpect(jsonPath("$.data.registeredCount").value(0));
    }

    @Test
    @DisplayName("an organizer cannot register for an event through the student endpoint")
    void organizerCannotUseStudentRegistrationEndpoint() throws Exception {
        mockMvc.perform(post("/api/events/" + eventId + "/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken))
                .andExpect(status().isForbidden());
    }
}
