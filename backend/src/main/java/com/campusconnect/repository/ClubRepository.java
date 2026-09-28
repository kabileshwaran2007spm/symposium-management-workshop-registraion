package com.campusconnect.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.campusconnect.model.Club;

public interface ClubRepository extends MongoRepository<Club, String> {

    Optional<Club> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
