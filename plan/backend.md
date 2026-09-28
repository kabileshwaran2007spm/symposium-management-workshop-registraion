# CampusConnect – Backend Documentation

## 1. Backend Overview

CampusConnect backend is a **Java Spring Boot REST API** responsible for authentication, authorization, event management, student registrations, seat management, attendance, notifications, analytics, and data export.

### Backend Stack

| Technology          | Purpose                          |
| ------------------- | -------------------------------- |
| Java                | Backend programming language     |
| Spring Boot         | Backend framework                |
| Spring Web          | REST API development             |
| Spring Security     | Authentication and authorization |
| JWT                 | Stateless authentication         |
| Spring Data MongoDB | MongoDB integration              |
| MongoDB             | Database                         |
| Maven               | Dependency management            |
| Bean Validation     | Request validation               |
| Postman             | API testing                      |

---

# 2. Backend Architecture

CampusConnect follows a layered architecture.

```text
                    React.js Frontend
                           │
                           │ HTTP / JSON
                           ▼
                 ┌─────────────────────┐
                 │   Spring Security   │
                 │      JWT Filter     │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │    Controller       │
                 │   REST Endpoints    │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │      Service        │
                 │  Business Logic     │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │     Repository      │
                 │ Spring Data MongoDB │
                 └──────────┬──────────┘
                            │
                            ▼
                 ┌─────────────────────┐
                 │      MongoDB        │
                 │     Database        │
                 └─────────────────────┘
```

---

# 3. Backend Responsibilities

The backend handles:

* User registration
* User login
* JWT generation
* Password encryption
* Role-based authorization
* Event creation
* Event approval
* Event modification
* Event deletion
* Event searching
* Event filtering
* Seat availability
* Student registration
* Duplicate registration prevention
* Registration cancellation
* Waitlist management
* QR pass generation
* Attendance
* Notifications
* Feedback
* Certificates
* Analytics
* CSV export
* JSON export
* Audit logging
