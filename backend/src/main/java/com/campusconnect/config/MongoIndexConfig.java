package com.campusconnect.config;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;

import com.campusconnect.model.Attendance;
import com.campusconnect.model.Club;
import com.campusconnect.model.Event;
import com.campusconnect.model.PasswordResetToken;
import com.campusconnect.model.Registration;
import com.campusconnect.model.RegistrationStatus;
import com.campusconnect.model.User;

/**
 * Declares every index explicitly at startup.
 *
 * <p>Indexes are created here rather than with {@code @Indexed} annotations for one
 * reason: the registration uniqueness rule needs a <em>partial</em> unique index,
 * and keeping all indexes in one readable place makes the data invariants easy to
 * audit.
 *
 * <p>The partial unique index on {@code (userId, eventId)} is what makes
 * double-registration impossible under concurrency. It is restricted to
 * seat-holding statuses so a student who cancels can register again later, while the
 * cancelled row stays in history for the "Cancelled" tab.
 */
@Configuration
public class MongoIndexConfig {

    private static final Logger log = LoggerFactory.getLogger(MongoIndexConfig.class);

    @Bean
    public ApplicationRunner ensureIndexes(MongoTemplate mongoTemplate) {
        return args -> {
            createUserIndexes(mongoTemplate.indexOps(User.class));
            createPasswordResetTokenIndexes(mongoTemplate.indexOps(PasswordResetToken.class));
            createEventIndexes(mongoTemplate.indexOps(Event.class));
            createRegistrationIndexes(mongoTemplate.indexOps(Registration.class));
            createAttendanceIndexes(mongoTemplate.indexOps(Attendance.class));
            createClubIndexes(mongoTemplate.indexOps(Club.class));
            log.info("MongoDB indexes verified");
        };
    }

    private void createPasswordResetTokenIndexes(IndexOperations tokens) {
        tokens.createIndex(new Index().on("tokenHash", Sort.Direction.ASC)
                .unique().named("uniq_password_reset_token_hash"));
        tokens.createIndex(new Index().on("expiresAt", Sort.Direction.ASC)
                .expire(Duration.ZERO).named("ttl_password_reset_tokens"));
    }

    private void createUserIndexes(IndexOperations users) {
        users.createIndex(new Index().on("email", Sort.Direction.ASC)
                .unique().named("uniq_users_email"));
        users.createIndex(new Index().on("studentId", Sort.Direction.ASC)
                .unique().named("uniq_users_student_id"));
        users.createIndex(new Index().on("role", Sort.Direction.ASC)
                .named("idx_users_role"));
    }

    private void createEventIndexes(IndexOperations events) {
        events.createIndex(new Index().on("status", Sort.Direction.ASC).named("idx_events_status"));
        events.createIndex(new Index().on("category", Sort.Direction.ASC).named("idx_events_category"));
        events.createIndex(new Index().on("department", Sort.Direction.ASC).named("idx_events_department"));
        events.createIndex(new Index().on("date", Sort.Direction.ASC).named("idx_events_date"));
        events.createIndex(new Index().on("organizerId", Sort.Direction.ASC).named("idx_events_organizer"));
    }

    private void createRegistrationIndexes(IndexOperations registrations) {
        registrations.createIndex(new Index().on("eventId", Sort.Direction.ASC)
                .named("idx_registrations_event"));
        registrations.createIndex(new Index().on("userId", Sort.Direction.ASC)
                .named("idx_registrations_user"));
        registrations.createIndex(new Index().on("qrToken", Sort.Direction.ASC)
                .unique().named("uniq_registrations_qr_token"));

        // The double-registration guard. Partial, so CANCELLED rows are ignored and
        // can never block a legitimate re-registration.
        registrations.createIndex(new Index()
                .on("userId", Sort.Direction.ASC)
                .on("eventId", Sort.Direction.ASC)
                .unique()
                .named("uniq_active_registration")
                .partial(PartialIndexFilter.of(
                        Criteria.where("status").in(RegistrationStatus.SEAT_HOLDING_NAMES))));
    }

    private void createAttendanceIndexes(IndexOperations attendance) {
        // Makes check-in idempotent at the database level.
        attendance.createIndex(new Index().on("registrationId", Sort.Direction.ASC)
                .unique().named("uniq_attendance_registration"));
        attendance.createIndex(new Index().on("eventId", Sort.Direction.ASC)
                .named("idx_attendance_event"));
    }

    private void createClubIndexes(IndexOperations clubs) {
        clubs.createIndex(new Index().on("name", Sort.Direction.ASC)
                .unique().named("uniq_clubs_name"));
    }
}
