# Product Requirements Document (PRD)

# CampusConnect – College Symposium & Workshop Management System

## 1. Document Information

| Field | Details |
|---|---|
| Product Name | CampusConnect |
| Project Type | College Event Management Platform |
| Document Type | Product Requirements Document |
| Version | 1.0 |
| Frontend | React.js |
| Backend | Java Spring Boot |
| Database | MongoDB |
| API | Spring Boot REST API |
| Authentication | JWT |
| Data Format | JSON |
| Target Users | Students, Event Organizers, College Administrators |

---

# 2. Product Overview

CampusConnect is a web-based college symposium and workshop management platform that provides a centralized system for discovering, publishing, registering, and managing college events.

The platform allows students to browse upcoming events, filter events by category and department, view real-time seat availability, and register for events.

Event organizers can create and manage events, monitor registrations, view attendees, and export attendee information in CSV or JSON format.

College administrators can manage users, clubs, events, registrations, and event approvals through an administrative dashboard.

The system is designed using React.js for the frontend, Spring Boot for REST APIs and business logic, and MongoDB for data storage.

---

# 3. Problem Statement

College symposiums, workshops, seminars, hackathons, and other events are often managed using multiple disconnected tools such as:

- Google Forms
- WhatsApp groups
- Instagram posts
- Excel spreadsheets
- Department websites
- Manual registration sheets

This creates several problems:

- Students may not know about all available events.
- Event information is scattered across different platforms.
- Students cannot easily filter events.
- Organizers manually manage registrations.
- Seat availability may not be updated accurately.
- Duplicate registrations can occur.
- Managing attendee lists is time-consuming.
- Attendance tracking may be manual.
- Administrators have limited centralized visibility.
- Generating event reports requires manual work.

CampusConnect solves these problems through a centralized event management platform.

---

# 4. Product Vision

To provide a single digital platform for colleges where students can discover and register for events while organizers and administrators can efficiently manage the complete event lifecycle.

---

# 5. Product Goals

## Primary Goals

1. Centralize college event information.
2. Simplify student event discovery.
3. Provide fast and easy event registration.
4. Display event seat availability.
5. Prevent duplicate registrations.
6. Provide organizers with attendee management tools.
7. Allow attendee data export in CSV and JSON.
8. Provide administrators with centralized event management.
9. Support secure role-based access.
10. Provide event analytics and reporting.

---

# 6. Target Users

## 6.1 Student

Students use the platform to:

- Create an account.
- Login securely.
- Browse events.
- Search events.
- Filter events.
- View event details.
- Check available seats.
- Register for events.
- Cancel registrations.
- View registered events.
- View QR registration pass.
- Receive notifications.
- Provide feedback.

---

## 6.2 Event Organizer

Event organizers may be:

- College clubs
- Department coordinators
- Faculty coordinators
- Student event teams

They can:

- Create events.
- Edit events.
- Submit events for approval.
- View registration statistics.
- View attendee lists.
- Export attendees.
- Manage event capacity.
- Mark attendance.
- View feedback.

---

## 6.3 Administrator

Administrators manage the complete platform.

They can:

- Manage users.
- Manage clubs.
- Manage departments.
- Approve or reject events.
- View all events.
- Manage registrations.
- View analytics.
- Generate reports.
- Manage platform settings.

---

# 7. User Roles

The system will have three primary roles.

```text
ADMIN
  |
  ├── Manage Users
  ├── Manage Clubs
  ├── Approve Events
  ├── View Reports
  └── View Analytics

ORGANIZER
  |
  ├── Create Events
  ├── Manage Events
  ├── View Attendees
  ├── Export Data
  └── Manage Attendance

STUDENT
  |
  ├── Browse Events
  ├── Search Events
  ├── Register
  ├── Cancel Registration
  ├── View My Events
  └── Submit Feedback