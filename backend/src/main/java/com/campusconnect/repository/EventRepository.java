package com.campusconnect.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.campusconnect.model.Event;
import com.campusconnect.model.EventStatus;

public interface EventRepository extends MongoRepository<Event, String>, EventQueryOperations {

    List<Event> findByOrganizerId(String organizerId);

    Page<Event> findByOrganizerId(String organizerId, Pageable pageable);

    long countByStatus(EventStatus status);

    long countByOrganizerId(String organizerId);

    List<Event> findByIdIn(Iterable<String> ids);

    long countByDateGreaterThanEqualAndStatus(LocalDate date, EventStatus status);
}
