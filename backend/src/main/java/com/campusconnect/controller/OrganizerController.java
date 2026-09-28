package com.campusconnect.controller;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.dto.ApiResponse;
import com.campusconnect.dto.AttendeeResponse;
import com.campusconnect.dto.EventAnalyticsResponse;
import com.campusconnect.dto.EventFilter;
import com.campusconnect.dto.EventResponse;
import com.campusconnect.dto.PageResponse;
import com.campusconnect.model.EventCategory;
import com.campusconnect.model.EventStatus;
import com.campusconnect.security.SecurityUtils;
import com.campusconnect.service.EventService;
import com.campusconnect.service.RegistrationService;

/**
 * Organizer workspace.
 *
 * <p>Every method is restricted to the organizer role, and each one additionally
 * verifies ownership inside the service, so an organizer can only ever reach their
 * own events. Administrators are allowed through for oversight, which the services
 * recognise and permit.
 */
@RestController
@RequestMapping("/api/organizer")
@PreAuthorize("hasAnyRole('ORGANIZER', 'ADMIN')")
public class OrganizerController {

    private final EventService eventService;
    private final RegistrationService registrationService;

    public OrganizerController(EventService eventService, RegistrationService registrationService) {
        this.eventService = eventService;
        this.registrationService = registrationService;
    }

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<PageResponse<EventResponse>>> myEvents(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EventCategory category,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        EventFilter filter = new EventFilter();
        filter.setSearch(search);
        filter.setCategory(category);
        filter.setStatus(status);

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Order.desc("createdAt")));

        return ResponseEntity.ok(ApiResponse.ok("Organizer events retrieved successfully",
                PageResponse.from(eventService.toResponsePage(
                        eventService.searchOrganizerEvents(SecurityUtils.currentUserId(), filter, pageable), true))));
    }

    @GetMapping("/events/{id}/attendees")
    public ResponseEntity<ApiResponse<PageResponse<AttendeeResponse>>> attendees(
            @PathVariable String id,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Order.asc("studentId")));

        return ResponseEntity.ok(ApiResponse.ok("Attendees retrieved successfully",
                PageResponse.from(registrationService.attendees(
                        id, SecurityUtils.currentUserId(), SecurityUtils.currentRole(), search, pageable))));
    }

    @GetMapping("/events/{id}/analytics")
    public ResponseEntity<ApiResponse<EventAnalyticsResponse>> analytics(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Analytics retrieved successfully",
                registrationService.analytics(id, SecurityUtils.currentUserId(), SecurityUtils.currentRole())));
    }

    @GetMapping("/events/{id}/export/csv")
    public ResponseEntity<String> exportCsv(@PathVariable String id) {
        String csv = registrationService.exportCsv(
                id, SecurityUtils.currentUserId(), SecurityUtils.currentRole());
        return download(csv, "event-" + id + "-attendees.csv");
    }

    @GetMapping("/events/{id}/export/json")
    public ResponseEntity<ApiResponse<List<AttendeeResponse.ExportRow>>> exportJson(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Attendees exported successfully",
                registrationService.exportRows(id, SecurityUtils.currentUserId(), SecurityUtils.currentRole())));
    }

    /**
     * CSV is a file download rather than a JSON envelope, so the browser can save it
     * directly. The filename is exposed through {@code Content-Disposition}, which
     * the CORS configuration makes readable to the frontend.
     */
    private ResponseEntity<String> download(String content, String filename) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(new MediaType("text", "csv", java.nio.charset.StandardCharsets.UTF_8))
                .body(content);
    }
}
