package com.campusconnect.model;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A college event (symposium, workshop, seminar, hackathon...).
 *
 * <p>{@code registeredCount} is the single source of truth for occupancy and is
 * only ever changed through the atomic helpers on {@code EventRepository}, which
 * is what makes it impossible for the count to exceed {@code capacity} when two
 * students register at the same moment.
 */
@Document(collection = "events")
public class Event {

    @Id
    private String id;

    private String title;

    private String description;

    private EventCategory category;

    private String department;

    private LocalDate date;

    private LocalTime startTime;

    private LocalTime endTime;

    private String venue;

    private int capacity;

    /** Number of seats currently held. Never exceeds {@link #capacity}. */
    private int registeredCount;

    private LocalDate registrationDeadline;

    private EventStatus status;

    /** Id of the {@code ORGANIZER} who owns this event. */
    private String organizerId;

    private Instant createdAt;

    private Instant updatedAt;

    public Event() {
    }

    /** Seats still available. Derived, never persisted, so it cannot drift. */
    public int getAvailableSeats() {
        return Math.max(0, capacity - registeredCount);
    }

    public boolean isFull() {
        return registeredCount >= capacity;
    }

    /**
     * "Almost full" as displayed by the seat-availability component. The UI
     * document shows the warning at 15 of 200 seats remaining but gives no
     * threshold, so it is pinned here at 10% of capacity.
     */
    public boolean isAlmostFull() {
        return !isFull() && getAvailableSeats() <= Math.ceil(capacity * 0.10);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public EventCategory getCategory() {
        return category;
    }

    public void setCategory(EventCategory category) {
        this.category = category;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public int getRegisteredCount() {
        return registeredCount;
    }

    public void setRegisteredCount(int registeredCount) {
        this.registeredCount = registeredCount;
    }

    public LocalDate getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(LocalDate registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
