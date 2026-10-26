package com.campusconnect.repository;

import java.time.Instant;
import java.util.Optional;

import com.campusconnect.model.Registration;

public interface RegistrationQueryOperations {

    Optional<Registration> cancelConfirmedRegistration(
            String registrationId, String userId, String eventId, Instant cancelledAt);
}
