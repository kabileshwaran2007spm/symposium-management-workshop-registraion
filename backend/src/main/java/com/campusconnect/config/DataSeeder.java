package com.campusconnect.config;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.campusconnect.model.Club;
import com.campusconnect.model.Event;
import com.campusconnect.model.EventCategory;
import com.campusconnect.model.EventStatus;
import com.campusconnect.model.Registration;
import com.campusconnect.model.RegistrationStatus;
import com.campusconnect.model.Role;
import com.campusconnect.model.User;
import com.campusconnect.repository.ClubRepository;
import com.campusconnect.repository.EventRepository;
import com.campusconnect.repository.RegistrationRepository;
import com.campusconnect.repository.UserRepository;

/**
 * Development seed data.
 *
 * <p>Public signup only creates students, so the organizer and administrator
 * accounts needed to demonstrate the approval and management flows have to come
 * from somewhere. This runner is that "somewhere", and it is off unless
 * {@code app.seed.enabled=true}.
 *
 * <p>Seeding is skipped entirely once any user exists, so it never overwrites real
 * data on a later start. The shared password comes from {@code APP_SEED_PASSWORD}
 * and is never hard-coded.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);

    private final UserRepository userRepository;
    private final ClubRepository clubRepository;
    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedPassword;

    public DataSeeder(
            UserRepository userRepository,
            ClubRepository clubRepository,
            EventRepository eventRepository,
            RegistrationRepository registrationRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.seed.password:}") String seedPassword) {
        this.userRepository = userRepository;
        this.clubRepository = clubRepository;
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedPassword = seedPassword;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (seedPassword == null || seedPassword.trim().length() < 8) {
            throw new IllegalStateException(
                    "Seeding is enabled but APP_SEED_PASSWORD is missing or shorter than 8 characters.");
        }

        if (userRepository.count() > 0) {
            log.info("Seed skipped: {} user(s) already exist", userRepository.count());
            return;
        }

        String hash = passwordEncoder.encode(seedPassword.trim());

        User admin = userRepository.save(user("Platform Administrator", "admin@campusconnect.edu",
                "ADM001", "Administration", Role.ADMIN, hash));
        User cseOrganizer = userRepository.save(user("Computer Science Club", "cse.club@campusconnect.edu",
                "FAC1001", "CSE", Role.ORGANIZER, hash));
        User eceOrganizer = userRepository.save(user("Robotics Club", "robotics.club@campusconnect.edu",
                "FAC1002", "ECE", Role.ORGANIZER, hash));

        List<User> students = new ArrayList<>();
        students.add(userRepository.save(user("Arun Kumar", "arun@campusconnect.edu",
                "CS2026001", "CSE", Role.STUDENT, hash)));
        students.add(userRepository.save(user("Priya Sharma", "priya@campusconnect.edu",
                "CS2026002", "CSE", Role.STUDENT, hash)));
        students.add(userRepository.save(user("Rahul Verma", "rahul@campusconnect.edu",
                "IT2026003", "IT", Role.STUDENT, hash)));
        students.add(userRepository.save(user("Sneha Iyer", "sneha@campusconnect.edu",
                "EC2026004", "ECE", Role.STUDENT, hash)));
        students.add(userRepository.save(user("Vikram Nair", "vikram@campusconnect.edu",
                "ME2026005", "MECH", Role.STUDENT, hash)));

        clubRepository.save(club("Computer Science Club", "CSE",
                "Runs workshops, hackathons and coding events for the CSE department."));
        clubRepository.save(club("Robotics Club", "ECE",
                "Builds and demonstrates robotics and embedded systems projects."));
        clubRepository.save(club("Literary Society", "ENG",
                "Hosts debates, quizzes and creative writing events."));

        LocalDate today = LocalDate.now();

        Event aiWorkshop = eventRepository.save(event(
                "AI and Machine Learning Workshop",
                "Hands-on introduction to machine learning concepts, covering supervised learning, "
                        + "model evaluation and a guided notebook exercise.",
                EventCategory.WORKSHOP, "CSE", today.plusDays(14),
                LocalTime.of(10, 0), LocalTime.of(13, 0), "Seminar Hall",
                200, today.plusDays(12), EventStatus.APPROVED, cseOrganizer.getId()));

        eventRepository.save(event(
                "National Level Hackathon 2026",
                "A 24-hour team hackathon across web, mobile and AI tracks with mentoring and "
                        + "judging by industry panel members.",
                EventCategory.HACKATHON, "CSE", today.plusDays(30),
                LocalTime.of(9, 0), LocalTime.of(18, 0), "Innovation Lab",
                120, today.plusDays(25), EventStatus.PENDING, cseOrganizer.getId()));

        eventRepository.save(event(
                "Robotics and Embedded Systems Seminar",
                "Overview of microcontroller programming, sensor integration and autonomous "
                        + "navigation with live demonstrations.",
                EventCategory.SEMINAR, "ECE", today.plusDays(21),
                LocalTime.of(14, 0), LocalTime.of(16, 30), "Auditorium",
                150, today.plusDays(19), EventStatus.APPROVED, eceOrganizer.getId()));

        eventRepository.save(event(
                "Technical Symposium 2026",
                "Department symposium with paper presentations, project expo and guest lectures. "
                        + "Programme is still being finalised.",
                EventCategory.SYMPOSIUM, "CSE", today.plusDays(45),
                LocalTime.of(9, 30), LocalTime.of(17, 0), "Main Auditorium",
                300, today.plusDays(40), EventStatus.DRAFT, eceOrganizer.getId()));

        // Register the first three students for the AI workshop so the attendee
        // table and analytics have something to show. registeredCount is set to
        // match the rows created, keeping the seat invariant intact.
        int registered = 0;
        for (int i = 0; i < 3; i++) {
            User student = students.get(i);
            Registration registration = new Registration();
            registration.setUserId(student.getId());
            registration.setEventId(aiWorkshop.getId());
            registration.setStatus(RegistrationStatus.CONFIRMED);
            registration.setRegisteredAt(Instant.now());
            registration.setQrToken(java.util.UUID.randomUUID().toString());
            registrationRepository.save(registration);
            registered++;
        }

        aiWorkshop.setRegisteredCount(registered);
        eventRepository.save(aiWorkshop);

        log.info("Seed complete: admin={}, organizers=[{}, {}], students={}, events=4, registrations={}",
                admin.getEmail(), cseOrganizer.getEmail(), eceOrganizer.getEmail(),
                students.size(), registered);
    }

    private User user(String name, String email, String studentId, String department,
                      Role role, String passwordHash) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setStudentId(studentId);
        user.setDepartment(department);
        user.setRole(role);
        user.setPassword(passwordHash);
        user.setCreatedAt(Instant.now());
        return user;
    }

    private Club club(String name, String department, String description) {
        Club club = new Club();
        club.setName(name);
        club.setDepartment(department);
        club.setDescription(description);
        club.setCreatedAt(Instant.now());
        return club;
    }

    private Event event(String title, String description, EventCategory category, String department,
                        LocalDate date, LocalTime startTime, LocalTime endTime, String venue,
                        int capacity, LocalDate deadline, EventStatus status, String organizerId) {
        Event event = new Event();
        event.setTitle(title);
        event.setDescription(description);
        event.setCategory(category);
        event.setDepartment(department);
        event.setDate(date);
        event.setStartTime(startTime);
        event.setEndTime(endTime);
        event.setVenue(venue);
        event.setCapacity(capacity);
        event.setRegisteredCount(0);
        event.setRegistrationDeadline(deadline);
        event.setStatus(status);
        event.setOrganizerId(organizerId);
        event.setCreatedAt(Instant.now());
        event.setUpdatedAt(Instant.now());
        return event;
    }
}
