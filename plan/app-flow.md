# CampusConnect – Application Flow

## 1. Application Overview

CampusConnect is a college symposium and workshop management system that allows students to discover and register for events, organizers to create and manage events, and administrators to approve events and monitor the complete platform.

### Technology Stack

* **Frontend:** React.js
* **Backend:** Java Spring Boot REST API
* **Database:** MongoDB
* **Authentication:** Spring Security + JWT
* **API Communication:** Axios
* **Data Format:** JSON

---

# 2. High-Level Application Flow

```text
                    ┌──────────────────────┐
                    │       User           │
                    │  Opens CampusConnect │
                    └──────────┬───────────┘
                               │
                               ▼
                    ┌──────────────────────┐
                    │      Landing Page    │
                    │  Login / Register    │
                    └──────────┬───────────┘
                               │
                    ┌──────────▼───────────┐
                    │    Authentication     │
                    │  Spring Security JWT  │
                    └──────────┬───────────┘
                               │
                 ┌─────────────┼─────────────┐
                 │             │             │
                 ▼             ▼             ▼
             Student       Organizer       Admin
                 │             │             │
                 ▼             ▼             ▼
          Student Dashboard  Organizer     Admin
                             Dashboard     Dashboard
```

---

# 3. User Registration Flow

```text
User
  │
  ▼
Register Page
  │
  ▼
Enter Details
  │
  ├── Name
  ├── Email
  ├── Student ID
  ├── Password
  ├── Department
  └── Role
  │
  ▼
React Validation
  │
  ▼
POST /api/auth/register
  │
  ▼
Spring Boot Controller
  │
  ▼
User Service
  │
  ▼
Check Existing Email / Student ID
  │
  ├── Already Exists ──► Error Message
  │
  └── New User
          │
          ▼
      Hash Password
          │
          ▼
      Save to MongoDB
          │
          ▼
      Registration Success
          │
          ▼
      Redirect to Login
```

---

# 4. Login Flow

```text
User
 │
 ▼
Login Page
 │
 ▼
Enter Email + Password
 │
 ▼
POST /api/auth/login
 │
 ▼
Spring Security
 │
 ▼
Validate Credentials
 │
 ├── Invalid ──► 401 Unauthorized
 │
 └── Valid
       │
       ▼
    Generate JWT
       │
       ▼
    Return Token
       │
       ▼
React stores authentication state
       │
       ▼
Check User Role
       │
 ┌─────┼──────────┐
 ▼     ▼          ▼
Student Organizer Admin
```

---

# 5. Student Application Flow

## 5.1 Student Dashboard

```text
Student Login
     │
     ▼
Student Dashboard
     │
     ├── Browse Events
     ├── Search Events
     ├── Filter Events
     ├── My Registrations
     ├── QR Pass
     ├── Notifications
     └── Profile
```

---

# 6. Event Discovery Flow

```text
Student Dashboard
       │
       ▼
   Events Page
       │
       ▼
┌─────────────────────────────┐
│ Search / Filter             │
│                             │
│ Category                    │
│ Department                  │
│ Date                        │
│ Event Type                  │
│ Availability                │
└──────────────┬──────────────┘
               │
               ▼
      GET /api/events
               │
               ▼
      Spring Boot API
               │
               ▼
           MongoDB
               │
               ▼
       Event List Returned
               │
               ▼
         Event Cards
```

---

# 7. Event Details Flow

```text
Student
   │
   ▼
Click Event
   │
   ▼
Event Details Page
   │
   ├── Event Name
   ├── Description
   ├── Organizer
   ├── Category
   ├── Date
   ├── Time
   ├── Venue
   ├── Capacity
   ├── Available Seats
   └── Registration Deadline
          │
          ▼
     Register Button
```

---

# 8. Event Registration Flow

```text
Student
   │
   ▼
Event Details
   │
   ▼
Click "Register"
   │
   ▼
JWT Authentication
   │
   ▼
Check Event
   │
   ├── Event Not Found ──► Error
   │
   ├── Registration Closed ──► Error
   │
   ├── Event Full ──► Waitlist / Error
   │
   ├── Already Registered ──► Error
   │
   └── Available
          │
          ▼
POST /api/events/{eventId}/register
          │
          ▼
     Registration Service
          │
          ▼
     Create Registration
          │
          ▼
      Update Seat Count
          │
          ▼
        MongoDB
          │
          ▼
   Registration Successful
          │
          ▼
     Generate QR Pass
          │
          ▼
   Show Confirmation
```

---

# 9. My Registrations Flow

```text
Student
   │
   ▼
My Registrations
   │
   ▼
GET /api/users/me/registrations
   │
   ▼
Spring Boot
   │
   ▼
MongoDB
   │
   ▼
Registered Events
   │
   ├── Upcoming Events
   ├── Completed Events
   └── Cancelled Events
```

---

# 10. Registration Cancellation Flow

```text
Student
   │
   ▼
My Registrations
   │
   ▼
Select Event
   │
   ▼
Cancel Registration
   │
   ▼
DELETE /api/events/{eventId}/register
   │
   ▼
Validate Registration
   │
   ▼
Cancel Registration
   │
   ▼
Release Seat
   │
   ▼
Check Waitlist
   │
   ├── Waitlist Available
   │       │
   │       ▼
   │   Promote Next Student
   │
   └── No Waitlist
           │
           ▼
      Seat Available
```

---

# 11. Organizer Flow

```text
Organizer Login
      │
      ▼
Organizer Dashboard
      │
      ├── Create Event
      ├── My Events
      ├── Edit Event
      ├── View Attendees
      ├── Attendance
      ├── Export Data
      └── Analytics
```

---

# 12. Create Event Flow

```text
Organizer
   │
   ▼
Create Event
   │
   ▼
Enter Event Details
   │
   ├── Event Name
   ├── Description
   ├── Category
   ├── Department
   ├── Date
   ├── Time
   ├── Venue
   ├── Capacity
   └── Registration Deadline
   │
   ▼
React Validation
   │
   ▼
POST /api/events
   │
   ▼
Spring Boot
   │
   ▼
Save Event
   │
   ▼
Status = PENDING
   │
   ▼
Admin Review
```

---

# 13. Event Approval Flow

```text
Organizer Creates Event
          │
          ▼
     PENDING
          │
          ▼
     Admin Review
          │
       ┌──┴──┐
       ▼     ▼
    Approve  Reject
       │       │
       ▼       ▼
   APPROVED  REJECTED
       │
       ▼
Event Visible to Students
```

---

# 14. Organizer Attendee Flow

```text
Organizer
    │
    ▼
My Events
    │
    ▼
Select Event
    │
    ▼
View Attendees
    │
    ▼
GET /api/organizer/events/{id}/attendees
    │
    ▼
Spring Boot
    │
    ▼
MongoDB
    │
    ▼
Attendee List
    │
    ├── Student Name
    ├── Student ID
    ├── Email
    ├── Department
    └── Registration Status
```

---

# 15. CSV / JSON Export Flow

```text
Organizer
    │
    ▼
Select Event
    │
    ▼
Attendee Management
    │
    ├───────────────┐
    ▼               ▼
Export CSV       Export JSON
    │               │
    ▼               ▼
Backend generates file/data
    │               │
    └───────┬───────┘
            ▼
        Download
```

### CSV API

```text
GET /api/organizer/events/{id}/export/csv
```

### JSON API

```text
GET /api/organizer/events/{id}/export/json
```

---

# 16. QR Code Attendance Flow

```text
Student Registers
       │
       ▼
QR Pass Generated
       │
       ▼
Student Arrives at Event
       │
       ▼
Organizer Scans QR
       │
       ▼
Validate QR
       │
       ├── Invalid ──► Reject
       │
       └── Valid
             │
             ▼
       Check Registration
             │
             ▼
       Mark Attendance
             │
             ▼
        MongoDB
             │
             ▼
       Attendance Success
```

---

# 17. Admin Flow

```text
Admin Login
    │
    ▼
Admin Dashboard
    │
    ├── Manage Users
    ├── Manage Events
    ├── Approve Events
    ├── Reject Events
    ├── Manage Clubs
    ├── View Registrations
    ├── View Attendance
    ├── Analytics
    └── Audit Logs
```

---

# 18. Admin Event Management Flow

```text
Admin
 │
 ▼
Event Management
 │
 ▼
View Pending Events
 │
 ├── Review Event Details
 │
 ├── Approve
 │      │
 │      ▼
 │   APPROVED
 │
 └── Reject
        │
        ▼
     REJECTED
```

---

# 19. Authentication and Authorization Flow

```text
React Application
       │
       ▼
Login
       │
       ▼
Spring Security
       │
       ▼
Validate Username + Password
       │
       ▼
Generate JWT
       │
       ▼
React stores Token
       │
       ▼
Request API
       │
       ▼
Authorization Header
       │
       │  Bearer <JWT>
       ▼
Spring Security Filter
       │
       ▼
Validate JWT
       │
       ├── Invalid ──► 401
       │
       ▼
Check Role
       │
       ├── Unauthorized Role ──► 403
       │
       ▼
Allow Request
```

---

# 20. Complete Registration Journey

```text
                    STUDENT
                       │
                       ▼
                Login / Register
                       │
                       ▼
                 Student Dashboard
                       │
                       ▼
                 Browse Events
                       │
                       ▼
                Search / Filter
                       │
                       ▼
                 Event Details
                       │
                       ▼
                  Register
                       │
                       ▼
              Check Availability
                       │
              ┌────────┴────────┐
              │                 │
          Available            Full
              │                 │
              ▼                 ▼
        Create Booking       Waitlist
              │
              ▼
        Generate QR Pass
              │
              ▼
        My Registrations
              │
              ▼
         Attend Event
              │
              ▼
          Scan QR Code
              │
              ▼
         Mark Attendance
              │
              ▼
        Feedback / Certificate
```

---

# 21. Complete Organizer Journey

```text
                  ORGANIZER
                     │
                     ▼
                   Login
                     │
                     ▼
            Organizer Dashboard
                     │
                     ▼
              Create Event
                     │
                     ▼
             Submit for Approval
                     │
                     ▼
                Admin Review
                     │
              ┌──────┴──────┐
              ▼             ▼
           Approved       Rejected
              │
              ▼
        Event Published
              │
              ▼
       Students Register
              │
              ▼
       View Attendee List
              │
              ▼
         Scan QR Codes
              │
              ▼
       Track Attendance
              │
              ▼
         Export Reports
              │
              ▼
            Analytics
```

---

# 22. Complete Admin Journey

```text
                     ADMIN
                       │
                       ▼
                     Login
                       │
                       ▼
                Admin Dashboard
                       │
          ┌────────────┼────────────┐
          ▼            ▼            ▼
       Users         Events       Clubs
          │            │
          │            ▼
          │      Pending Events
          │            │
          │       ┌────┴────┐
          │       ▼         ▼
          │    Approve     Reject
          │       │
          │       ▼
          │   Published
          │
          └───────────────┐
                          ▼
                    Analytics
                          │
                          ▼
                     Audit Logs
```

---

# 23. Backend Request Flow

Every protected API request follows this flow:

```text
React Component
      │
      ▼
Axios Service
      │
      ▼
JWT Token
      │
      ▼
Spring Security
      │
      ▼
Controller
      │
      ▼
Service
      │
      ▼
Repository
      │
      ▼
MongoDB
      │
      ▼
Repository
      │
      ▼
Service
      │
      ▼
Controller
      │
      ▼
JSON Response
      │
      ▼
React UI
```

---

# 24. Error Handling Flow

```text
User Request
     │
     ▼
Frontend Validation
     │
     ├── Invalid ──► Display Error
     │
     ▼
Backend Validation
     │
     ├── Invalid ──► 400 Bad Request
     │
     ▼
Authentication
     │
     ├── Failed ──► 401 Unauthorized
     │
     ▼
Authorization
     │
     ├── Failed ──► 403 Forbidden
     │
     ▼
Resource Check
     │
     ├── Not Found ──► 404 Not Found
     │
     ▼
Business Logic
     │
     ├── Duplicate ──► 409 Conflict
     │
     ▼
Success
     │
     ▼
200 / 201 Response
```

---

# 25. Overall System Flow

```text
                         CAMPUSCONNECT
                              │
             ┌────────────────┼────────────────┐
             │                │                │
             ▼                ▼                ▼
          STUDENT         ORGANIZER           ADMIN
             │                │                │
             ▼                ▼                ▼
       Discover Events    Create Events    Manage Platform
             │                │                │
             ▼                ▼                ▼
       View Details       Submit Event      Approve Event
             │                │                │
             ▼                ▼                │
         Register          Approval ◄─────────┘
             │                │
             ▼                ▼
        QR Pass          Publish Event
             │                │
             └───────┬────────┘
                     ▼
                Attend Event
                     │
                     ▼
                QR Check-in
                     │
                     ▼
                Attendance
                     │
                     ▼
             Feedback / Certificate
                     │
                     ▼
                  Reports
                     │
                     ▼
                 Analytics
```

---

# 26. Final Application Flow Summary

```text
REGISTER
   ↓
LOGIN
   ↓
ROLE IDENTIFICATION
   ↓
┌──────────────────────────────────────┐
│                                      │
│ STUDENT                              │
│ ↓                                    │
│ Browse → Search → Filter → Register  │
│ → QR Pass → Attend → Feedback        │
│                                      │
├──────────────────────────────────────┤
│                                      │
│ ORGANIZER                            │
│ ↓                                    │
│ Create → Submit → Approval           │
│ → Manage Attendees → Attendance      │
│ → Export → Analytics                 │
│                                      │
├──────────────────────────────────────┤
│                                      │
│ ADMIN                                │
│ ↓                                    │
│ Manage Users → Review Events         │
│ → Approve/Reject → Analytics         │
│ → Audit Logs                         │
│                                      │
└──────────────────────────────────────┘
```

## 27. Technology Flow

```text
┌──────────────────────────────┐
│       React.js Frontend      │
│                              │
│ Pages + Components + Axios   │
└───────────────┬──────────────┘
                │
                │ REST API / JSON
                ▼
┌──────────────────────────────┐
│      Spring Boot Backend     │
│                              │
│ Controller                   │
│      ↓                       │
│ Service                      │
│      ↓                       │
│ Repository                   │
└───────────────┬──────────────┘
                │
                │ Spring Data MongoDB
                ▼
┌──────────────────────────────┐
│           MongoDB            │
│                              │
│ Users                        │
│ Events                       │
│ Registrations                │
│ Clubs                        │
│ Attendance                   │
│ Notifications                │
│ Feedback                     │
│ Certificates                 │
│ Audit Logs                   │
└──────────────────────────────┘
```

## 28. Main Flow in One Line

**User → Register/Login → Role-Based Dashboard → Discover/Create/Manage Events → Event Approval → Registration → QR Pass → Attendance → Feedback/Certificate → Reports & Analytics**

