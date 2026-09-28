package com.campusconnect.dto;

import java.util.Map;

import com.campusconnect.model.EventStatus;
import com.campusconnect.model.RegistrationStatus;
import com.campusconnect.model.Role;

/**
 * Platform-wide metrics for the admin dashboard, which shows user, event and
 * registration totals plus the pending-approval count.
 */
public record AdminAnalyticsResponse(
        long totalUsers,
        Map<Role, Long> usersByRole,
        long totalEvents,
        long pendingEvents,
        Map<EventStatus, Long> eventsByStatus,
        long totalRegistrations,
        Map<RegistrationStatus, Long> registrationsByStatus,
        long totalClubs,
        long totalAttendance) {
}
