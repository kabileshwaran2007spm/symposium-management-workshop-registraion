package com.campusconnect.model;

/**
 * Event category. The UI design document presents category chips for
 * Workshop / Symposium / Technical, and the data model shows {@code WORKSHOP},
 * so the enum covers those plus the other event types named in the problem
 * statement (seminars, hackathons, cultural events).
 */
public enum EventCategory {
    WORKSHOP,
    SYMPOSIUM,
    SEMINAR,
    HACKATHON,
    TECHNICAL,
    CULTURAL
}
