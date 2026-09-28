package com.campusconnect.controller;

import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.dto.ApiResponse;
import com.campusconnect.dto.EventAvailability;
import com.campusconnect.dto.EventFilter;
import com.campusconnect.dto.EventRequest;
import com.campusconnect.dto.EventResponse;
import com.campusconnect.dto.PageResponse;
import com.campusconnect.model.Event;
import com.campusconnect.model.EventCategory;
import com.campusconnect.security.SecurityUtils;
import com.campusconnect.service.EventService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private static final int MAX_PAGE_SIZE = 100;

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /**
     * Public discovery. Only approved events are returned, and the visibility
     * ceiling is applied in the service rather than trusting a query parameter.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<EventResponse>>> listEvents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EventCategory category,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(required = false) EventAvailability availability,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {

        EventFilter filter = new EventFilter();
        filter.setSearch(search);
        filter.setCategory(category);
        filter.setDepartment(department);
        filter.setDate(date);
        filter.setAvailability(availability);

        Pageable pageable = pageable(page, size);
        return ResponseEntity.ok(ApiResponse.ok("Events retrieved successfully",
                PageResponse.from(eventService.toResponsePage(
                        eventService.searchPublicEvents(filter, pageable), true))));
    }

    /** Event details. Unpublished events are visible only to their owner or an admin. */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<EventResponse>> getEvent(@PathVariable String id) {
        Event event = eventService.getVisibleEvent(id, currentUserIdOrNull(), SecurityUtils.currentRole());
        return ResponseEntity.ok(ApiResponse.ok("Event retrieved successfully",
                eventService.toResponse(event, true)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<EventResponse>> createEvent(@Valid @RequestBody EventRequest request) {
        EventResponse created = eventService.createEvent(request, SecurityUtils.currentUserId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Event created and submitted for approval", created));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<EventResponse>> updateEvent(
            @PathVariable String id, @Valid @RequestBody EventRequest request) {
        EventResponse updated = eventService.updateEvent(id, request, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.ok("Event updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ORGANIZER')")
    public ResponseEntity<ApiResponse<Void>> deleteEvent(@PathVariable String id) {
        String message = eventService.deleteEvent(id, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.ok(message, null));
    }

    /** Upcoming events first, then by start time. */
    private Pageable pageable(int page, int size) {
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int safePage = Math.max(page, 0);
        return PageRequest.of(safePage, safeSize, Sort.by(
                Sort.Order.asc("date"), Sort.Order.asc("startTime")));
    }

    private String currentUserIdOrNull() {
        try {
            return SecurityUtils.currentUserId();
        } catch (RuntimeException ex) {
            // Anonymous browsing is allowed for public events.
            return null;
        }
    }
}
