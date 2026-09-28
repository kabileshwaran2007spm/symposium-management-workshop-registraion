package com.campusconnect.dto;

import java.time.Instant;

import com.campusconnect.model.Club;

public record ClubResponse(String id, String name, String department, String description, Instant createdAt) {

    public static ClubResponse from(Club club) {
        return new ClubResponse(
                club.getId(), club.getName(), club.getDepartment(), club.getDescription(), club.getCreatedAt());
    }
}
