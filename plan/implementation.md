# CampusConnect – Implementation Document

## 1. Implementation Overview

CampusConnect is implemented as a full-stack web application using:

* **Frontend:** React.js
* **Backend:** Java Spring Boot
* **Database:** MongoDB
* **Authentication:** Spring Security + JWT
* **API Communication:** REST API + JSON
* **HTTP Client:** Axios
* **Build Tool:** Maven
* **Testing:** JUnit + Mockito + Postman
* **Version Control:** Git + GitHub

The implementation follows a layered architecture where the React frontend communicates with the Spring Boot backend through REST APIs.

---

# 2. System Architecture

```text
                    ┌─────────────────────┐
                    │      User           │
                    │ Student / Organizer │
                    │       / Admin       │
                    └──────────┬──────────┘
                               │
                               ▼
                    ┌─────────────────────┐
                    │     React.js        │
                    │     Frontend        │
                    └──────────┬──────────┘
                               │
                         REST API / JSON
                               │
                               ▼
                    ┌─────────────────────┐
                    │    Spring Boot      │
                    │      Backend        │
                    ├─────────────────────┤
                    │ Controller          │
                    │ Service             │
                    │ Repository          │
                    │ Security            │
                    └──────────┬──────────┘
                               │
                       Spring Data MongoDB
                               │
                               ▼
                    ┌─────────────────────┐
                    │      MongoDB        │
                    └─────────────────────┘
```

---

# 3. Development Environment

## Required Software

| Software                | Purpose                 |
| ----------------------- | ----------------------- |
| Java JDK 17+            | Backend development     |
| IntelliJ IDEA / Eclipse | Spring Boot development |
| Node.js + npm           | React development       |
| VS Code                 | Frontend development    |
| MongoDB                 | Database                |
| MongoDB Compass         | Database visualization  |
| Postman                 | API testing             |
| Git                     | Version control         |
| GitHub                  | Repository hosting      |

---

# 4. Project Structure

The project is divided into two major applications.

```text
CampusConnect/
│
├── frontend/
│
└── backend/
```

---

# 5. Frontend Structure

```text
frontend/
│
├── public/
│
├── src/
│   │
│   ├── assets/
│   │
│   ├── components/
│   │   ├── Navbar.jsx
│   │   ├── Sidebar.jsx
│   │   ├── EventCard.jsx
│   │   ├── SearchBar.jsx
│   │   ├── Filter.jsx
│   │   ├── Button.jsx
│   │   ├── Modal.jsx
│   │   ├── Toast.jsx
│   │   └── LoadingSpinner.jsx
│   │
│   ├── pages/
│   │   ├── Home.jsx
│   │   ├── Login.jsx
│   │   ├── Register.jsx
│   │   ├── Events.jsx
│   │   ├── EventDetails.jsx
│   │   ├── MyRegistrations.jsx
│   │   ├── StudentDashboard.jsx
│   │   ├── OrganizerDashboard.jsx
│   │   ├── AdminDashboard.jsx
│   │   ├── CreateEvent.jsx
│   │   ├── Attendees.jsx
│   │   └── Analytics.jsx
│   │
│   ├── services/
│   │   ├── api.js
│   │   ├── authService.js
│   │   ├── eventService.js
│   │   ├── registrationService.js
│   │   └── adminService.js
│   │
│   ├── context/
│   │   └── AuthContext.jsx
│   │
│   ├── routes/
│   │   └── AppRoutes.jsx
│   │
│   ├── utils/
│   │   └── validation.js
│   │
│   ├── App.jsx
│   └── main.jsx
│
├── package.json
└── .env
```

---

# 6. Backend Structure

```text
backend/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── campusconnect/
│   │   │
│   │   │           ├── CampusConnectApplication.java
│   │   │           │
│   │   │           ├── controller/
│   │   │           │   ├── AuthController.java
│   │   │           │   ├── EventController.java
│   │   │           │   ├── RegistrationController.java
│   │   │           │   ├── OrganizerController.java
│   │   │           │   ├── AdminController.java
│   │   │           │   └── AttendanceController.java
│   │   │           │
│   │   │           ├── service/
│   │   │           │   ├── AuthService.java
│   │   │           │   ├── EventService.java
│   │   │           │   ├── RegistrationService.java
│   │   │           │   ├── AttendanceService.java
│   │   │           │   └── AdminService.java
│   │   │           │
│   │   │           ├── repository/
│   │   │           │   ├── UserRepository.java
│   │   │           │   ├── EventRepository.java
│   │   │           │   ├── RegistrationRepository.java
│   │   │           │   └── AttendanceRepository.java
│   │   │           │
│   │   │           ├── model/
│   │   │           │   ├── User.java
│   │   │           │   ├── Event.java
│   │   │           │   ├── Registration.java
│   │   │           │   └── Attendance.java
│   │   │           │
│   │   │           ├── dto/
│   │   │           │   ├── LoginRequest.java
│   │   │           │   ├── RegisterRequest.java
│   │   │           │   ├── EventRequest.java
│   │   │           │   └── RegistrationResponse.java
│   │   │           │
│   │   │           ├── security/
│   │   │           │   ├── JwtService.java
│   │   │           │   ├── JwtAuthenticationFilter.java
│   │   │           │   └── SecurityConfig.java
│   │   │           │
│   │   │           ├── exception/
│   │   │           │   ├── GlobalExceptionHandler.java
│   │   │           │   ├── ResourceNotFoundException.java
│   │   │           │   └── DuplicateRegistrationException.java
│   │   │           │
│   │   │           └── config/
│   │   │               └── CorsConfig.java
│   │   │
│   │   └── resources/
│   │       └── application.properties
│   │
│   └── test/
│
├── pom.xml
└── README.md
```

---

# 7. Database Implementation

MongoDB is used as the primary database.

Database name:

```text
campusconnect
```

Main collections:

```text
users
events
registrations
clubs
attendance
notifications
feedback
certificates
auditLogs
```

---

# 8. User Model

The User collection stores authentication and profile information.

Example:

```json
{
  "name": "Arun Kumar",
  "email": "arun@example.com",
  "studentId": "CS2026001",
  "password": "encrypted-password",
  "department": "CSE",
  "role": "STUDENT",
  "createdAt": "2026-09-28T10:00:00"
}
```

### Roles

```text
STUDENT
ORGANIZER
ADMIN
```

---

# 9. Event Model

Example event document:

```json
{
  "title": "AI and Machine Learning Workshop",
  "description": "Introduction to machine learning concepts.",
  "category": "WORKSHOP",
  "department": "CSE",
  "date": "2026-10-12",
  "startTime": "10:00",
  "endTime": "13:00",
  "venue": "Seminar Hall",
  "capacity": 200,
  "registeredCount": 120,
  "registrationDeadline": "2026-10-10",
  "status": "APPROVED",
  "organizerId": "organizer-id"
}
```

---

# 10. Registration Model

```json
{
  "userId": "user-id",
  "eventId": "event-id",
  "status": "CONFIRMED",
  "registeredAt": "2026-09-28T10:30:00",
  "qrToken": "unique-qr-token"
}
```

Possible registration statuses:

```text
CONFIRMED
CANCELLED
WAITLISTED
ATTENDED
```

---

# 11. Authentication Implementation

Authentication uses:

```text
Spring Security
        +
JWT
        +
BCrypt
```

### Registration

```text
User
 ↓
Register Form
 ↓
React
 ↓
POST /api/auth/register
 ↓
Spring Boot
 ↓
Validate Data
 ↓
Hash Password
 ↓
Save User
 ↓
MongoDB
```

The original password should never be stored directly.

---

# 12. Login Implementation

```text
Email + Password
       ↓
POST /api/auth/login
       ↓
Find User
       ↓
Verify Password
       ↓
Generate JWT
       ↓
Return JWT
       ↓
React Authentication State
```

Example response:

```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "jwt-token",
    "role": "STUDENT"
  }
}
```

---

# 13. JWT Authorization

For protected requests, the frontend sends:

```text
Authorization: Bearer <JWT>
```

Backend flow:

```text
Request
   ↓
JWT Filter
   ↓
Validate Token
   ↓
Extract User
   ↓
Check Role
   ↓
Controller
```

---

# 14. Role-Based Access

## Student

Can:

```text
View events
Search events
Filter events
Register
Cancel registration
View registrations
View QR pass
View notifications
Submit feedback
```

## Organizer

Can:

```text
Create events
Edit events
Delete events
View attendees
Manage attendance
Export data
View analytics
```

## Admin

Can:

```text
Manage users
Review events
Approve events
Reject events
Manage clubs
View registrations
View analytics
View audit logs
```

---

# 15. Event Creation Implementation

Organizer submits the event form.

```text
Create Event Form
       ↓
POST /api/events
       ↓
JWT Validation
       ↓
Check Organizer Role
       ↓
Validate Event Data
       ↓
Create Event
       ↓
status = PENDING
       ↓
Save MongoDB
```

The event does not immediately become visible to students if the approval workflow is enabled.

---

# 16. Event Approval Implementation

Admin receives pending events.

```text
PENDING
   │
   ├──── Approve ────► APPROVED
   │
   └──── Reject ─────► REJECTED
```

Approved events become available for student registration.

---

# 17. Event Search Implementation

Students can search using event information.

Example:

```text
GET /api/events?search=machine
```

Possible filters:

```text
category
department
date
status
availability
```

Example:

```text
GET /api/events?category=WORKSHOP&department=CSE
```

---

# 18. Event Registration Implementation

Registration follows several validation steps.

```text
Student clicks Register
          ↓
Check JWT
          ↓
Find Event
          ↓
Check Event Status
          ↓
Check Registration Deadline
          ↓
Check Existing Registration
          ↓
Check Available Seats
          ↓
Create Registration
          ↓
Increment registeredCount
          ↓
Generate QR Token
          ↓
Return Success
```

---

# 19. Duplicate Registration Prevention

Before creating a registration:

```text
Find:
userId + eventId
```

If a confirmed registration already exists:

```text
409 Conflict
```

Response:

```json
{
  "success": false,
  "message": "You are already registered for this event"
}
```

---

# 20. Seat Management

Available seats:

```text
availableSeats =
capacity - registeredCount
```

Example:

```text
Capacity = 200
Registered = 120

Available = 200 - 120
          = 80
```

The UI displays:

```text
80 seats available
```

---

# 21. Full Event Handling

If:

```text
registeredCount >= capacity
```

registration should not proceed.

Response:

```json
{
  "success": false,
  "message": "Event is full"
}
```

If waitlist functionality is enabled:

```text
Event Full
    ↓
Add Student to Waitlist
```

---

# 22. Registration Cancellation

Flow:

```text
Student
   ↓
My Registrations
   ↓
Cancel
   ↓
DELETE /api/events/{eventId}/register
   ↓
Validate Registration
   ↓
Set Status = CANCELLED
   ↓
Release Seat
   ↓
Check Waitlist
```

If a waitlist exists, the next eligible student can be promoted according to the application's waitlist rules.

---

# 23. QR Pass Implementation

After successful registration, the backend creates a unique QR token.

```text
Registration
      ↓
Generate Unique Token
      ↓
Store Token
      ↓
React receives QR data
      ↓
Display QR Code
```

The QR code should represent a non-sensitive registration identifier/token rather than exposing unnecessary personal information.

---

# 24. Attendance Implementation

Organizer scans the student's QR code.

```text
QR Scanner
    ↓
QR Token
    ↓
POST /api/attendance/check-in
    ↓
Validate Token
    ↓
Find Registration
    ↓
Check Event
    ↓
Check Existing Attendance
    ↓
Create Attendance
    ↓
Success
```

Example attendance document:

```json
{
  "registrationId": "registration-id",
  "eventId": "event-id",
  "userId": "user-id",
  "checkedInAt": "2026-10-12T10:15:00",
  "status": "PRESENT"
}
```

---

# 25. CSV Export Implementation

Organizer can export attendee information.

```text
Organizer
   ↓
Select Event
   ↓
GET /api/organizer/events/{id}/export/csv
   ↓
Validate Organizer
   ↓
Fetch Registrations
   ↓
Generate CSV
   ↓
Return File
```

Example CSV:

```text
Student ID,Name,Email,Department,Status
CS001,Arun,arun@example.com,CSE,CONFIRMED
CS002,Kumar,kumar@example.com,IT,ATTENDED
```

---

# 26. JSON Export Implementation

Endpoint:

```text
GET /api/organizer/events/{id}/export/json
```

Example:

```json
[
  {
    "studentId": "CS001",
    "name": "Arun",
    "email": "arun@example.com",
    "department": "CSE",
    "status": "CONFIRMED"
  }
]
```

---

# 27. REST API Implementation

## Authentication

| Method | Endpoint             | Purpose       |
| ------ | -------------------- | ------------- |
| POST   | `/api/auth/register` | Register user |
| POST   | `/api/auth/login`    | Login         |
| POST   | `/api/auth/logout`   | Logout        |

## Events

| Method | Endpoint           | Purpose      |
| ------ | ------------------ | ------------ |
| GET    | `/api/events`      | Get events   |
| GET    | `/api/events/{id}` | Get event    |
| POST   | `/api/events`      | Create event |
| PUT    | `/api/events/{id}` | Update event |
| DELETE | `/api/events/{id}` | Delete event |

## Registration

| Method | Endpoint                         | Purpose          |
| ------ | -------------------------------- | ---------------- |
| POST   | `/api/events/{eventId}/register` | Register         |
| DELETE | `/api/events/{eventId}/register` | Cancel           |
| GET    | `/api/users/me/registrations`    | My registrations |

## Organizer

| Method | Endpoint                                 | Purpose          |
| ------ | ---------------------------------------- | ---------------- |
| GET    | `/api/organizer/events`                  | Organizer events |
| GET    | `/api/organizer/events/{id}/attendees`   | Attendees        |
| GET    | `/api/organizer/events/{id}/analytics`   | Analytics        |
| GET    | `/api/organizer/events/{id}/export/csv`  | CSV              |
| GET    | `/api/organizer/events/{id}/export/json` | JSON             |

## Admin

| Method | Endpoint                         | Purpose   |
| ------ | -------------------------------- | --------- |
| GET    | `/api/admin/users`               | Users     |
| GET    | `/api/admin/events`              | Events    |
| PUT    | `/api/admin/events/{id}/approve` | Approve   |
| PUT    | `/api/admin/events/{id}/reject`  | Reject    |
| GET    | `/api/admin/analytics`           | Analytics |

## Attendance

| Method | Endpoint                                  | Purpose           |
| ------ | ----------------------------------------- | ----------------- |
| POST   | `/api/attendance/check-in`                | Mark attendance   |
| GET    | `/api/events/{eventId}/attendance`        | View attendance   |
| GET    | `/api/events/{eventId}/attendance/export` | Export attendance |

---

# 28. Standard API Response

Successful response:

```json
{
  "success": true,
  "message": "Event retrieved successfully",
  "data": {}
}
```

Error response:

```json
{
  "success": false,
  "message": "Event not found",
  "data": null
}
```

---

# 29. HTTP Status Codes

| Code | Meaning                                  |
| ---- | ---------------------------------------- |
| 200  | Successful request                       |
| 201  | Resource created                         |
| 204  | Successful request with no response body |
| 400  | Invalid request                          |
| 401  | Unauthorized                             |
| 403  | Forbidden                                |
| 404  | Resource not found                       |
| 409  | Conflict                                 |
| 500  | Server error                             |

---

# 30. Validation

Backend validation should be implemented even if frontend validation exists.

Example:

```text
Event Name
    ↓
Required?
    ↓
Minimum length?
    ↓
Valid?
```

Validation should be applied to:

* Email
* Password
* Student ID
* Event name
* Event date
* Event capacity
* Registration deadline
* Required fields

---

# 31. Exception Handling

A global exception handler should provide consistent API errors.

```text
Controller
    ↓
Exception
    ↓
GlobalExceptionHandler
    ↓
Standard JSON Response
```

Example:

```json
{
  "success": false,
  "message": "Registration already exists"
}
```

---

# 32. CORS Configuration

Since React and Spring Boot run separately during development, CORS must allow the frontend origin.

Example development architecture:

```text
React
localhost:5173
      │
      │ API Request
      ▼
Spring Boot
localhost:8080
```

CORS should be restricted to the required frontend origins rather than allowing every origin in production.

---

# 33. Environment Configuration

Configuration should not be hard-coded.

Example:

```text
application.properties
```

Conceptual configuration:

```properties
spring.data.mongodb.uri=${MONGODB_URI}
spring.data.mongodb.database=campusconnect

server.port=8080

jwt.secret=${JWT_SECRET}
```

Sensitive values should be supplied through environment variables or a secure configuration mechanism.

---

# 34. Frontend-Backend Connection

React uses Axios to communicate with Spring Boot.

```text
React Component
      ↓
Service
      ↓
Axios
      ↓
Spring Boot REST API
      ↓
MongoDB
```

Example conceptual request:

```javascript
axios.get("/api/events");
```

---

# 35. Authentication Request Flow

```text
React Login
     ↓
Axios
     ↓
POST /api/auth/login
     ↓
Spring Boot
     ↓
Spring Security
     ↓
MongoDB User
     ↓
Password Verification
     ↓
JWT Generated
     ↓
React
```

---

# 36. Protected Route Implementation

Frontend routes should check authentication and role.

Example:

```text
/admin/dashboard
       ↓
Is logged in?
       ↓
Is ADMIN?
       ↓
Allow Access
```

If not authorized:

```text
Redirect → Login / Unauthorized Page
```

Backend authorization remains the final security boundary; frontend route protection alone is not sufficient.

---

# 37. Business Logic Implementation

The main business rules are implemented inside service classes.

Example:

```text
RegistrationService
       │
       ├── validateEvent()
       ├── checkDeadline()
       ├── checkDuplicate()
       ├── checkCapacity()
       ├── createRegistration()
       ├── updateSeatCount()
       └── generateQrToken()
```

Controllers should remain thin and delegate business logic to services.

---

# 38. Event Status Flow

```text
DRAFT
  │
  ▼
PENDING
  │
  ├──────────────┐
  ▼              ▼
APPROVED       REJECTED
  │
  ▼
PUBLISHED
  │
  ▼
COMPLETED
```

Possible implementation status values:

```text
DRAFT
PENDING
APPROVED
REJECTED
CANCELLED
COMPLETED
```

---

# 39. Registration Status Flow

```text
REGISTER
    │
    ▼
CONFIRMED
    │
    ├──── Cancel ────► CANCELLED
    │
    ▼
ATTENDED
```

When capacity is unavailable:

```text
REGISTER
    │
    ▼
WAITLISTED
    │
    ▼
PROMOTED
    │
    ▼
CONFIRMED
```

---

# 40. Analytics Implementation

Organizer analytics can calculate:

```text
Total Capacity
Total Registrations
Available Seats
Attendance Count
Attendance Percentage
Cancellation Count
Waitlist Count
```

Example:

```text
Capacity       = 200
Registrations  = 180
Attendance     = 150

Attendance % =
150 / 180 × 100
= 83.33%
```

---

# 41. Notification Implementation

Notifications can be created for events such as:

```text
Registration successful
Registration cancelled
Event approved
Event rejected
Event reminder
Waitlist promotion
Certificate available
```

Example:

```json
{
  "userId": "user-id",
  "message": "Your registration for AI Workshop is confirmed.",
  "type": "REGISTRATION",
  "read": false
}
```

---

# 42. Audit Log Implementation

Important administrative actions should be recorded.

Examples:

```text
Admin approved event
Admin rejected event
Organizer created event
Organizer deleted event
Admin changed user role
```

Example:

```json
{
  "userId": "admin-id",
  "action": "APPROVE_EVENT",
  "targetId": "event-id",
  "timestamp": "2026-09-28T12:00:00"
}
```

---

# 43. Database Indexing

Indexes should be created for frequently queried fields.

Recommended indexes:

```text
users.email
users.studentId

events.category
events.department
events.date
events.status

registrations.userId
registrations.eventId
```

For duplicate registration prevention, the `userId + eventId` combination should have a uniqueness strategy appropriate to the MongoDB implementation.

---

# 44. Security Implementation

Security requirements include:

```text
✓ Password hashing
✓ JWT authentication
✓ Role-based authorization
✓ Protected endpoints
✓ Server-side validation
✓ Ownership checks
✓ Restricted CORS
✓ Secure environment variables
✓ Consistent error handling
```

Sensitive information should not be returned unnecessarily in API responses.

---

# 45. Testing Implementation

## Unit Testing

Test individual services:

```text
AuthService
EventService
RegistrationService
AttendanceService
```

Example cases:

```text
Valid registration
Duplicate registration
Event full
Invalid event
Expired registration deadline
Unauthorized user
```

---

# 46. API Testing

Postman should be used to test:

```te
```
