package com.campusconnect.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Public signup payload.
 *
 * <p>There is deliberately no {@code role} field. The application-flow document
 * lists "Role" on the registration form, but accepting it would let anyone grant
 * themselves {@code ADMIN}, which contradicts the security requirements. Public
 * signup therefore always creates a {@code STUDENT}; organizer and administrator
 * accounts are provisioned separately.
 */
public record RegisterRequest(

        @NotBlank(message = "Name is required")
        @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid address")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email,

        @NotBlank(message = "Student ID is required")
        @Size(min = 3, max = 30, message = "Student ID must be between 3 and 30 characters")
        @Pattern(regexp = "^[A-Za-z0-9-]+$",
                message = "Student ID may contain only letters, digits and hyphens")
        String studentId,

        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
        String password,

        @NotBlank(message = "Department is required")
        @Size(max = 60, message = "Department must be at most 60 characters")
        String department) {
}
