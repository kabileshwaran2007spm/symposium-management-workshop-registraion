# Technical Requirements Document (TRD)

# CampusConnect – College Symposium & Workshop Management System

## 1. Document Information

| Field | Details |
|---|---|
| Project Name | CampusConnect |
| Document | Technical Requirements Document |
| Version | 1.0 |
| Frontend | React.js |
| Backend | Java Spring Boot |
| Database | MongoDB |
| API Architecture | REST API |
| Authentication | JWT |
| API Data Format | JSON |
| Version Control | Git + GitHub |
| API Testing | Postman |

---

# 2. Technical Overview

CampusConnect is a full-stack web application for managing college symposiums, workshops, seminars, hackathons, and other academic or extracurricular events.

The system follows a three-layer architecture:

```text
┌──────────────────────────────┐
│        React Frontend        │
│                              │
│ Student UI                   │
│ Organizer Dashboard          │
│ Admin Dashboard              │
└──────────────┬───────────────┘
               │
               │ REST API / JSON
               ↓
┌──────────────────────────────┐
│       Spring Boot API        │
│                              │
│ Controllers                  │
│ Services                     │
│ Security                     │
│ Validation                   │
│ Business Logic               │
└──────────────┬───────────────┘
               │
               │ Spring Data MongoDB
               ↓
┌──────────────────────────────┐
│           MongoDB            │
│                              │
│ Users                        │
│ Events                       │
│ Registrations                │
│ Clubs                        │
│ Attendance                   │
│ Notifications                │
└──────────────────────────────┘