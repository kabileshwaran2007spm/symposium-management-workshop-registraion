package com.campusconnect;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;

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
    public void setUp() {
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

    @Test
    @DisplayName("public discovery supports allowlisted event sorting")
    void publicDiscoverySupportsSorting() throws Exception {
        Event existingEvent = eventRepository.findById(eventId).orElseThrow();
        existingEvent.setTitle("Zebra Robotics Workshop");
        eventRepository.save(existingEvent);

        Event laterEvent = new Event();
        laterEvent.setTitle("Aardvark Robotics Seminar");
        laterEvent.setDescription("A seminar about robotics and automation.");
        laterEvent.setCategory(EventCategory.SEMINAR);
        laterEvent.setDepartment("ECE");
        laterEvent.setDate(LocalDate.now().plusDays(20));
        laterEvent.setStartTime(LocalTime.of(14, 0));
        laterEvent.setEndTime(LocalTime.of(16, 0));
        laterEvent.setVenue("Auditorium");
        laterEvent.setCapacity(80);
        laterEvent.setRegisteredCount(0);
        laterEvent.setRegistrationDeadline(LocalDate.now().plusDays(15));
        laterEvent.setStatus(EventStatus.APPROVED);
        laterEvent.setOrganizerId(userRepository.findByEmailIgnoreCase("cse@test.edu").orElseThrow().getId());
        laterEvent.setCreatedAt(Instant.now());
        laterEvent.setUpdatedAt(Instant.now());
        eventRepository.save(laterEvent);

        mockMvc.perform(get("/api/events?sort=TITLE&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Aardvark Robotics Seminar"));

        mockMvc.perform(get("/api/events?sort=LATEST_DATE&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].title").value("Aardvark Robotics Seminar"));
    }

    @Test
    @DisplayName("users can read and update only the editable fields in their own profile")
    void studentCanUpdateOwnProfileWithoutChangingProtectedFields() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("arun@test.edu"))
                .andExpect(jsonPath("$.data.password").doesNotExist());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Arun Updated",
                                  "department": "ECE",
                                  "role": "ADMIN",
                                  "studentId": "OTHER-ID",
                                  "email": "other@test.edu",
                                  "password": "new-password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Arun Updated"))
                .andExpect(jsonPath("$.data.department").value("ECE"))
                .andExpect(jsonPath("$.data.role").value("STUDENT"))
                .andExpect(jsonPath("$.data.studentId").value("CS2026001"))
                .andExpect(jsonPath("$.data.email").value("arun@test.edu"))
                .andExpect(jsonPath("$.data.password").doesNotExist());

        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Arun Updated"))
                .andExpect(jsonPath("$.data.department").value("ECE"));
    }

    @Test
    @DisplayName("profile reads and writes require authentication and validate editable fields")
    void profileRequiresAuthenticationAndValidFields() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \",\"department\":\"CSE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("password reset requests are public but do not reveal whether an email exists")
    void passwordResetEndpointsArePublic() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown@test.edu\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("If an account exists for that email, a password reset link has been sent."));

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"not-a-valid-token\",\"password\":\"Password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "This reset link is invalid or expired. Request a new one."));
    }

    @Test
    @DisplayName("Google login endpoint is public and reports missing provider configuration")
    void googleLoginEndpointIsPublic() throws Exception {
        mockMvc.perform(post("/api/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"credential\":\"test-google-token\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Google sign-in is not configured."));
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
                .map(event -> Objects.requireNonNull(event).getId())
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
