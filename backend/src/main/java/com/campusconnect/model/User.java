package com.campusconnect.model;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * A platform user.
 *
 * <p>The {@code password} field always stores a BCrypt hash. Password hashes are
 * never returned to clients: controllers and services only ever emit
 * {@code UserResponse}, which has no password field.
 *
 * <p>Indexes for {@code email} and {@code studentId} are declared in
 * {@code MongoIndexConfig} rather than with annotations, so that index creation
 * is explicit and includes the partial unique index used for registrations.
 */
@Document(collection = "users")
public class User {

    @Id
    private String id;

    private String name;

    private String email;

    /**
     * Institutional identifier. Required for every user, including organizers and
     * administrators (who are given staff identifiers), so the unique index never
     * has to deal with null values.
     */
    private String studentId;

    private String password;

    private String department;

    private Role role;

    private int tokenVersion;

    private Instant createdAt;

    public User() {
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public int getTokenVersion() {
        return tokenVersion;
    }

    public void setTokenVersion(int tokenVersion) {
        this.tokenVersion = tokenVersion;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public boolean isOrganizer() {
        return role == Role.ORGANIZER;
    }
}
