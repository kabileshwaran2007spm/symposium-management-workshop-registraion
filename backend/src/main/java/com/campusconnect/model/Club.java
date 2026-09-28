package com.campusconnect.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A student club or department body that runs events.
 *
 * <p>The admin API contract requires {@code GET /api/admin/clubs}, but none of
 * the planning documents define a club model. This is the minimal shape needed to
 * satisfy that endpoint; clubs are intentionally not wired into {@link Event},
 * because the documented event model has no club reference and inventing that
 * relationship is out of scope.
 */
@Document(collection = "clubs")
public class Club {

    @Id
    private String id;

    private String name;

    private String department;

    private String description;

    private Instant createdAt;

    public Club() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
