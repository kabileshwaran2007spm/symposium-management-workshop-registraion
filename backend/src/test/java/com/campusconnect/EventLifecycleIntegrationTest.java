package com.campusconnect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;

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
import org.springframework.test.web.servlet.MvcResult;

import com.campusconnect.model.Role;
import com.campusconnect.model.User;
import com.campusconnect.repository.AttendanceRepository;
import com.campusconnect.repository.ClubRepository;
import com.campusconnect.repository.EventRepository;
import com.campusconnect.repository.RegistrationRepository;
import com.campusconnect.repository.UserRepository;
import com.campusconnect.security.JwtService;

import com.jayway.jsonpath.JsonPath;

/**
 * The full documented event lifecycle, exercised through the real HTTP stack.
 *
 * <p>This is the end-to-end guarantee for the core business flow in the plan:
 * an organizer creates an event, it is invisible to students until an administrator
 * approves it, students then register (with duplicate protection and seat
 * accounting), the QR pass is checked in exactly once, and analytics agree with
 * everything that happened. If this class passes, the platform's reason to exist
 * works.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EventLifecycleIntegrationTest {

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
    private String studentAToken;
    private String studentBToken;
    private String studentCToken;

    @BeforeEach
    public void setUp() {
        attendanceRepository.deleteAll();
        registrationRepository.deleteAll();
        eventRepository.deleteAll();
        clubRepository.deleteAll();
        userRepository.deleteAll();

        adminToken = jwtService.generateToken(
                user("Admin", "admin@lifecycle.test", "ADM001", Role.ADMIN));
        organizerToken = jwtService.generateToken(
                user("CSE Club", "cse@lifecycle.test", "FAC001", Role.ORGANIZER));
        studentAToken = jwtService.generateToken(
                user("Arun", "arun@lifecycle.test", "CS2026001", Role.STUDENT));
        studentBToken = jwtService.generateToken(
                user("Priya", "priya@lifecycle.test", "CS2026002", Role.STUDENT));
        studentCToken = jwtService.generateToken(
                user("Rahul", "rahul@lifecycle.test", "IT2026003", Role.STUDENT));
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

    /** Small capacity (2) so the full-event branch of the flow is reachable. */
    private String createEventViaApi(String title) throws Exception {
        String payload = """
                {
                  "title": "%s",
                  "description": "Lifecycle verification event with a meaningful description.",
                  "category": "SEMINAR",
                  "department": "CSE",
                  "date": "%s",
                  "startTime": "10:00",
                  "endTime": "12:00",
                  "venue": "Audit Hall",
                  "capacity": 2,
                  "registrationDeadline": "%s"
                }
                """.formatted(title, LocalDate.now().plusDays(20), LocalDate.now().plusDays(18));

        MvcResult result = mockMvc.perform(post("/api/events")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
    }

    // ------------------------------------------------------------------
    // The documented lifecycle, in order
    // ------------------------------------------------------------------

    @Test
    @DisplayName("create -> hidden -> approve -> public -> register -> dup-409 -> "
            + "cancel -> release -> re-register -> full -> check-in -> analytics")
    void fullLifecycleBehavesAsDocumented() throws Exception {
        String eventId = createEventViaApi("Lifecycle Audit Event");

        // --- Pending events are invisible to students and anonymous visitors ---
        mockMvc.perform(get("/api/events?size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == '" + eventId + "')]").isEmpty());

        mockMvc.perform(get("/api/events/" + eventId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                .andExpect(status().isNotFound());

        // The organizer sees their own pending event.
        mockMvc.perform(get("/api/events/" + eventId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        // --- Registration before approval is refused (409: conflicts with state) ---
        mockMvc.perform(post("/api/events/" + eventId + "/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));

        // --- Admin approves; the event becomes public ---
        mockMvc.perform(put("/api/admin/events/" + eventId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/api/events?size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[?(@.id == '" + eventId + "')]").isNotEmpty());

        // --- Student A registers and receives a QR token ---
        MvcResult registration = mockMvc.perform(post("/api/events/" + eventId + "/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.qrToken").isNotEmpty())
                .andReturn();

        String responseBody = registration.getResponse().getContentAsString();
        String qrToken = JsonPath.read(responseBody, "$.data.qrToken");

        // Duplicate attempt conflicts and the message matches the documented copy.
        mockMvc.perform(post("/api/events/" + eventId + "/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message")
                        .value("You are already registered for this event"));

        // Seat accounting moved with the registration: 1 of 2 taken.
        mockMvc.perform(get("/api/events/" + eventId))
                .andExpect(jsonPath("$.data.registeredCount").value(1))
                .andExpect(jsonPath("$.data.availableSeats").value(1));

        // --- Cancelling releases the seat ---
        mockMvc.perform(delete("/api/events/" + eventId + "/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/events/" + eventId))
                .andExpect(jsonPath("$.data.registeredCount").value(0));

        // A cancelled registration can register again. This creates a NEW row
        // with a NEW QR token; the old token belongs to a cancelled registration.
        MvcResult reregistration = mockMvc.perform(post("/api/events/" + eventId + "/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                .andExpect(status().isCreated())
                .andReturn();

        String currentToken = JsonPath.read(
                reregistration.getResponse().getContentAsString(), "$.data.qrToken");
        assertThat(currentToken).isNotEqualTo(qrToken);

        // --- Fill the event, then prove the guard fires ---
        mockMvc.perform(post("/api/events/" + eventId + "/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentBToken))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/events/" + eventId + "/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentCToken))
                .andExpect(status().isConflict());

        // --- QR check-in: once, idempotently, by the owning organizer ---
        // The stale token from the cancelled registration is refused at the door.
        mockMvc.perform(post("/api/attendance/check-in")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrToken\": \"" + qrToken + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This registration has been cancelled"));

        // The current token checks in exactly once.
        mockMvc.perform(post("/api/attendance/check-in")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrToken\": \"" + currentToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkedInCount").value(1));

        mockMvc.perform(post("/api/attendance/check-in")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"qrToken\": \"" + currentToken + "\"}"))
                .andExpect(status().isConflict());

        // --- Analytics agree with everything that happened ---
        mockMvc.perform(get("/api/organizer/events/" + eventId + "/analytics")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.capacity").value(2))
                .andExpect(jsonPath("$.data.registrations").value(2))
                .andExpect(jsonPath("$.data.availableSeats").value(0))
                .andExpect(jsonPath("$.data.confirmed").value(1))
                .andExpect(jsonPath("$.data.attended").value(1))
                .andExpect(jsonPath("$.data.attendanceCount").value(1))
                .andExpect(jsonPath("$.data.cancelled").value(1));
    }

    // ------------------------------------------------------------------
    // Rejection branch
    // ------------------------------------------------------------------

    @Test
    @DisplayName("a rejected event stays hidden and cannot be rejected twice")
    void rejectionBranchBehaves() throws Exception {
        String eventId = createEventViaApi("Lifecycle Reject Event");

        mockMvc.perform(put("/api/admin/events/" + eventId + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"));

        mockMvc.perform(get("/api/events/" + eventId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                .andExpect(status().isNotFound());

        // Rejection only applies to PENDING events; this one is REJECTED.
        mockMvc.perform(put("/api/admin/events/" + eventId + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }

    // ------------------------------------------------------------------
    // Organizer resubmission of a rejected event
    // ------------------------------------------------------------------

    @Test
    @DisplayName("an organizer edits a rejected event back to PENDING")
    void organizerResubmitsRejectedEvent() throws Exception {
        String eventId = createEventViaApi("Lifecycle Resubmit Event");

        mockMvc.perform(put("/api/admin/events/" + eventId + "/reject")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        String payload = """
                {
                  "title": "Lifecycle Resubmit Event (fixed)",
                  "description": "The venue was corrected and the event is being resubmitted.",
                  "category": "SEMINAR",
                  "department": "CSE",
                  "date": "%s",
                  "startTime": "10:00",
                  "endTime": "12:00",
                  "venue": "Audit Hall B",
                  "capacity": 2,
                  "registrationDeadline": "%s",
                  "status": "PENDING"
                }
                """.formatted(LocalDate.now().plusDays(20), LocalDate.now().plusDays(18));

        mockMvc.perform(put("/api/events/" + eventId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    // ------------------------------------------------------------------
    // Exports agree with the table
    // ------------------------------------------------------------------

    @Test
    @DisplayName("CSV export contains the documented header row and one row per registration")
    void csvExportMatchesAttendeeTable() throws Exception {
        String eventId = createEventViaApi("Lifecycle Export Event");

        mockMvc.perform(put("/api/admin/events/" + eventId + "/approve")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/events/" + eventId + "/register")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + studentAToken))
                .andExpect(status().isCreated());

        String csv = mockMvc.perform(get("/api/organizer/events/" + eventId + "/export/csv")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + organizerToken))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(csv).startsWith("Student ID,Name,Email,Department,Status");
        // Header + student A's row.
        assertThat(csv.lines().count()).isEqualTo(2);
        assertThat(csv).contains("arun@lifecycle.test");
    }

    // ------------------------------------------------------------------
    // Public JSON contract sanity
    // ------------------------------------------------------------------

    @Test
    @DisplayName("error responses always use the documented envelope")
    void errorEnvelopeIsConsistent() throws Exception {
        // Asserted on the raw body because JSON null needs an exact-shape check:
        // the documented envelope carries "data": null on errors.
        mockMvc.perform(get("/api/events/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("\"data\":null")));
    }
}
