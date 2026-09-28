package com.campusconnect.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.campusconnect.dto.AdminAnalyticsResponse;
import com.campusconnect.dto.AdminRegistrationResponse;
import com.campusconnect.dto.ApiResponse;
import com.campusconnect.dto.ClubResponse;
import com.campusconnect.dto.EventFilter;
import com.campusconnect.dto.EventResponse;
import com.campusconnect.dto.PageResponse;
import com.campusconnect.dto.UserResponse;
import com.campusconnect.model.EventCategory;
import com.campusconnect.model.EventStatus;
import com.campusconnect.model.RegistrationStatus;
import com.campusconnect.model.Role;
import com.campusconnect.service.AdminService;

/**
 * Administrative surface. The class-level rule means every endpoint here requires
 * the ADMIN role, enforced by Spring Security on the server rather than by hiding
 * buttons in the UI.
 */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> users(
            @RequestParam(required = false) Role role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<UserResponse> users = adminService.listUsers(role, pageable(page, size, "createdAt"));
        return ResponseEntity.ok(ApiResponse.ok("Users retrieved successfully", PageResponse.from(users)));
    }

    @GetMapping("/events")
    public ResponseEntity<ApiResponse<PageResponse<EventResponse>>> events(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) EventCategory category,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        EventFilter filter = new EventFilter();
        filter.setSearch(search);
        filter.setStatus(status);
        filter.setCategory(category);
        filter.setDepartment(department);
        filter.setDate(date);

        return ResponseEntity.ok(ApiResponse.ok("Events retrieved successfully",
                PageResponse.from(adminService.listEvents(filter, pageable(page, size, "createdAt")))));
    }

    /** The approval queue the admin dashboard opens on. */
    @GetMapping("/events/pending")
    public ResponseEntity<ApiResponse<PageResponse<EventResponse>>> pendingEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok("Pending events retrieved successfully",
                PageResponse.from(adminService.pendingEvents(pageable(page, size, "createdAt")))));
    }

    @GetMapping("/registrations")
    public ResponseEntity<ApiResponse<PageResponse<AdminRegistrationResponse>>> registrations(
            @RequestParam(required = false) String eventId,
            @RequestParam(required = false) RegistrationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(ApiResponse.ok("Registrations retrieved successfully",
                adminService.listRegistrations(eventId, status, pageable(page, size, "registeredAt"))));
    }

    @GetMapping("/clubs")
    public ResponseEntity<ApiResponse<List<ClubResponse>>> clubs() {
        return ResponseEntity.ok(ApiResponse.ok("Clubs retrieved successfully", adminService.listClubs()));
    }

    @PutMapping("/events/{id}/approve")
    public ResponseEntity<ApiResponse<EventResponse>> approve(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Event approved", adminService.approve(id)));
    }

    @PutMapping("/events/{id}/reject")
    public ResponseEntity<ApiResponse<EventResponse>> reject(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.ok("Event rejected", adminService.reject(id)));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<AdminAnalyticsResponse>> analytics() {
        return ResponseEntity.ok(ApiResponse.ok("Analytics retrieved successfully", adminService.analytics()));
    }

    /** Newest first, clamped so a hand-crafted size cannot ask for the whole table. */
    private Pageable pageable(int page, int size, String sortProperty) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Order.desc(sortProperty)));
    }
}
