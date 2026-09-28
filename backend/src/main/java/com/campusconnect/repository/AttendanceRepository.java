package com.campusconnect.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.campusconnect.model.Attendance;

public interface AttendanceRepository extends MongoRepository<Attendance, String> {

    /** Drives check-in idempotency: a registration may be checked in once. */
    boolean existsByRegistrationId(String registrationId);

    List<Attendance> findByEventIdOrderByCheckedInAtAsc(String eventId);

    List<Attendance> findByEventIdIn(List<String> eventIds);

    long countByEventId(String eventId);

    long countByUserId(String userId);
}
