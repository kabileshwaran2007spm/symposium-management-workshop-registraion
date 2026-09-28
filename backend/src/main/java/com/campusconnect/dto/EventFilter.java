package com.campusconnect.dto;

import java.time.LocalDate;
import java.util.Set;

import com.campusconnect.model.EventCategory;
import com.campusconnect.model.EventStatus;

/**
 * Everything that can narrow an event listing.
 *
 * <p>A {@code null} field means "no constraint". {@code allowedStatuses} is the
 * caller's visibility ceiling rather than a user-visible filter: public browsing
 * passes only {@code APPROVED}, an organizer passes their own events' statuses,
 * and an administrator passes {@code null} to see everything.
 */
public class EventFilter {

    private String search;

    private EventCategory category;

    private String department;

    private LocalDate date;

    private EventAvailability availability;

    private EventStatus status;

    private String organizerId;

    private Set<EventStatus> allowedStatuses;

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
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

    public EventAvailability getAvailability() {
        return availability;
    }

    public void setAvailability(EventAvailability availability) {
        this.availability = availability;
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

    public Set<EventStatus> getAllowedStatuses() {
        return allowedStatuses;
    }

    public void setAllowedStatuses(Set<EventStatus> allowedStatuses) {
        this.allowedStatuses = allowedStatuses;
    }
}
