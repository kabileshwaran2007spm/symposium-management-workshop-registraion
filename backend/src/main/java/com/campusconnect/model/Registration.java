package com.campusconnect.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A student's registration for an event.
 *
 * <p>Cancelling sets {@link RegistrationStatus#CANCELLED} instead of deleting the
 * row, because "My Registrations" has a Cancelled tab. The partial unique index
 * on {@code (userId, eventId)} therefore only covers seat-holding statuses, which
 * keeps the double-registration rule without permanently shadow-banning a
 * student who changed their mind.
 */
@Document(collection = "registrations")
public class Registration {

    @Id
    private String id;

    private String userId;

    private String eventId;

    private RegistrationStatus status;

    private Instant registeredAt;

    private Instant cancelledAt;

    /**
     * Opaque token embedded in the QR pass. It identifies a registration without
     * exposing personal data, which is what the QR pass is scanned against.
     */
    private String qrToken;

    public Registration() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public RegistrationStatus getStatus() {
        return status;
    }

    public void setStatus(RegistrationStatus status) {
        this.status = status;
    }

    public Instant getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(Instant registeredAt) {
        this.registeredAt = registeredAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getQrToken() {
        return qrToken;
    }

    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }
}
