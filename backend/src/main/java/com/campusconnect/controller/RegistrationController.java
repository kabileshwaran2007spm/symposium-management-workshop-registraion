package com.campusconnect.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.dto.ApiResponse;
import com.campusconnect.dto.RegistrationResponse;
import com.campusconnect.security.SecurityUtils;
import com.campusconnect.service.RegistrationService;

/**
 * Registration endpoints.
 *
 * <p>The student id always comes from the bearer token, never from the path or
 * body, so one student cannot act on another's registrations.
 */
@RestController
@RequestMapping("/api")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/events/{eventId}/register")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<RegistrationResponse>> register(@PathVariable String eventId) {
        RegistrationResponse registration =
                registrationService.register(SecurityUtils.currentUserId(), eventId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful", registration));
    }

    @DeleteMapping("/events/{eventId}/register")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<Void>> cancel(@PathVariable String eventId) {
        String message = registrationService.cancel(SecurityUtils.currentUserId(), eventId);
        return ResponseEntity.ok(ApiResponse.ok(message, null));
    }

    @GetMapping("/users/me/registrations")
    public ResponseEntity<ApiResponse<List<RegistrationResponse>>> myRegistrations() {
        return ResponseEntity.ok(ApiResponse.ok("Registrations retrieved successfully",
                registrationService.myRegistrations(SecurityUtils.currentUserId())));
    }
}
