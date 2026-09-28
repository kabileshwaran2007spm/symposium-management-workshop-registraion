package com.campusconnect.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.campusconnect.model.Role;
import com.campusconnect.model.User;

public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByStudentIdIgnoreCase(String studentId);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByStudentIdIgnoreCase(String studentId);

    List<User> findByIdIn(Iterable<String> ids);

    Page<User> findByRole(Role role, Pageable pageable);

    long countByRole(Role role);
}
