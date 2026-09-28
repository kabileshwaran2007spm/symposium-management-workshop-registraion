package com.campusconnect.dto;

import java.time.Instant;

import com.campusconnect.model.Role;
import com.campusconnect.model.User;

/**
 * Client-facing user view.
 *
 * <p>Having its own type is what guarantees a password hash can never leak: the
 * {@code User} document is never serialized directly.
 */
public record UserResponse(
        String id,
        String name,
        String email,
        String studentId,
        String department,
        Role role,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getStudentId(),
                user.getDepartment(),
                user.getRole(),
                user.getCreatedAt());
    }
}
