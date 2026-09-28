package com.campusconnect.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Check-in payload. The organizer supplies the opaque token read from the
 * student's QR pass; no personal data travels in the QR code itself.
 */
public record CheckInRequest(

        @NotBlank(message = "QR token is required")
        String qrToken) {
}
