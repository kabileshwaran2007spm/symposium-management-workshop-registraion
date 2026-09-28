package com.campusconnect.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * An attendance record created when an organizer scans a student's QR pass.
 *
 * <p>A unique index on {@code registrationId} makes check-in idempotent: a second
 * scan is rejected by the database rather than creating a duplicate record.
 */
@Document(collection = "attendance")
public class Attendance {

    @Id
    private String id;

    private String registrationId;

    private String eventId;

    private String userId;

    private Instant checkedInAt;

    private AttendanceStatus status;

    public Attendance() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(String registrationId) {
        this.registrationId = registrationId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Instant getCheckedInAt() {
        return checkedInAt;
    }

    public void setCheckedInAt(Instant checkedInAt) {
        this.checkedInAt = checkedInAt;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
    }
}
