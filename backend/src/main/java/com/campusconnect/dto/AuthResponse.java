package com.campusconnect.dto;

import com.campusconnect.model.Role;
import com.campusconnect.model.User;

/**
 * Login result. Matches the documented shape ({@code token} plus role) and adds
 * the profile fields the UI needs to render a dashboard without a second call.
 */
public record AuthResponse(
        String token,
        String tokenType,
        long expiresInSeconds,
        String userId,
        String name,
        String email,
        String studentId,
        String department,
        Role role) {

    public static AuthResponse of(String token, long expiresInSeconds, User user) {
        return new AuthResponse(
                token,
                "Bearer",
                expiresInSeconds,
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getStudentId(),
                user.getDepartment(),
                user.getRole());
    }
}
