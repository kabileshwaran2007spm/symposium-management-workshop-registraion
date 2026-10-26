package com.campusconnect.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.campusconnect.model.Registration;
import com.campusconnect.model.RegistrationStatus;

public interface RegistrationRepository
        extends MongoRepository<Registration, String>, RegistrationQueryOperations {

    Optional<Registration> findByQrToken(String qrToken);

    /**
     * The student's newest registration for an event, whatever its status.
     * Used by cancellation so an already-cancelled row produces a meaningful
     * conflict instead of a misleading "not found".
     */
    Optional<Registration> findFirstByUserIdAndEventIdOrderByRegisteredAtDesc(String userId, String eventId);

    /** The single seat-holding registration, if the student has one. */
    @Query("{ 'userId': ?0, 'eventId': ?1, 'status': { $in: ['CONFIRMED', 'WAITLISTED', 'ATTENDED'] } }")
    Optional<Registration> findActiveRegistration(String userId, String eventId);

    boolean existsByUserIdAndEventIdAndStatusIn(
            String userId, String eventId, Collection<RegistrationStatus> statuses);

    Page<Registration> findByEventId(String eventId, Pageable pageable);

    List<Registration> findByEventIdOrderByRegisteredAtAsc(String eventId);

    List<Registration> findByEventIdInOrderByRegisteredAtAsc(Collection<String> eventIds);

    List<Registration> findByUserIdOrderByRegisteredAtDesc(String userId);

    long countByEventId(String eventId);

    long countByEventIdAndStatus(String eventId, RegistrationStatus status);

    long countByUserId(String userId);

    long countByStatus(RegistrationStatus status);
}
