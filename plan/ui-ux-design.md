# CampusConnect – UI/UX Design Document

## 1. UI/UX Design Overview

CampusConnect is a college symposium and workshop management platform designed to make event discovery, registration, event management, and attendance simple and efficient.

The UI/UX design focuses on:

* Simple navigation
* Fast event discovery
* Clear event information
* Easy one-click registration
* Role-based dashboards
* Responsive design
* Accessible interface
* Consistent visual design
* Clear feedback and error messages

---

# 2. Design Goals

### Primary Goals

1. Make events easy to discover.
2. Make registration quick and simple.
3. Clearly display available seats.
4. Provide separate experiences for Students, Organizers, and Admins.
5. Reduce unnecessary steps.
6. Provide clear confirmation after important actions.
7. Make the interface responsive on desktop, tablet, and mobile.
8. Maintain consistent design across all pages.

---

# 3. Target Users

CampusConnect has three primary user types.

| User      | Main Requirements                |
| --------- | -------------------------------- |
| Student   | Discover and register for events |
| Organizer | Create and manage events         |
| Admin     | Approve and monitor events       |

---

# 4. User Experience Principles

## 4.1 Simplicity

The interface should avoid unnecessary elements.

Example:

```text
Event Card
───────────────
Web Development Workshop

📅 12 Oct 2026
🕐 10:00 AM
📍 Seminar Hall

120 / 200 Seats Available

[View Details]
```

---

## 4.2 Visibility

Important information should be immediately visible.

For events:

* Event name
* Date
* Time
* Venue
* Category
* Available seats
* Registration status

---

## 4.3 Consistency

Buttons, colors, spacing, typography, cards, and navigation should remain consistent throughout the application.

---

## 4.4 Feedback

The system should immediately tell users when an action succeeds or fails.

Examples:

```text
✓ Registration successful!
```

```text
⚠ This event is already full.
```

```text
✓ Event created successfully and submitted for approval.
```

---

## 4.5 Error Prevention

The UI should prevent common mistakes.

Examples:

* Disable registration when event is full.
* Prevent duplicate registration.
* Confirm before cancellation.
* Validate required fields.
* Show registration deadline.

---

# 5. Information Architecture

```text
CampusConnect
│
├── Landing Page
│
├── Authentication
│   ├── Login
│   └── Register
│
├── Student
│   ├── Dashboard
│   ├── Events
│   ├── Event Details
│   ├── My Registrations
│   ├── QR Pass
│   ├── Notifications
│   └── Profile
│
├── Organizer
│   ├── Dashboard
│   ├── Create Event
│   ├── My Events
│   ├── Event Details
│   ├── Attendees
│   ├── Attendance
│   ├── Analytics
│   └── Export
│
└── Admin
    ├── Dashboard
    ├── Users
    ├── Events
    ├── Event Approval
    ├── Clubs
    ├── Registrations
    ├── Analytics
    └── Audit Logs
```

---

# 6. Navigation Structure

## Student Navigation

```text
┌─────────────────────────────────────────────────────┐
│ CampusConnect    Events  My Registrations  Profile  │
│                                      🔔     👤       │
└─────────────────────────────────────────────────────┘
```

## Organizer Navigation

```text
┌─────────────────────────────────────────────────────┐
│ CampusConnect    Dashboard  Events  Attendees       │
│                          Analytics     Profile       │
└─────────────────────────────────────────────────────┘
```

## Admin Navigation

```text
┌─────────────────────────────────────────────────────┐
│ CampusConnect    Dashboard  Users  Events  Reports  │
│                          Analytics    Settings       │
└─────────────────────────────────────────────────────┘
```

---

# 7. Design System

## 7.1 Color Strategy

The application should use a professional academic/technology visual style.

Recommended semantic colors:

| Purpose   | Usage                                  |
| --------- | -------------------------------------- |
| Primary   | Main buttons, navigation, links        |
| Secondary | Secondary actions                      |
| Success   | Registration success, available seats  |
| Warning   | Pending approval, approaching deadline |
| Error     | Validation errors, rejected events     |
| Neutral   | Backgrounds, borders, secondary text   |

Colors should be defined centrally so they can be changed without modifying individual components.

---

# 8. Typography

Recommended typography hierarchy:

```text
H1
Page Title

H2
Section Heading

H3
Card Heading

Body
Normal content

Caption
Secondary information
```

Example:

```text
College Symposium 2026
        ↓
Main heading

Upcoming Events
        ↓
Section heading

AI & Machine Learning Workshop
        ↓
Event title

Learn the fundamentals of machine learning...
        ↓
Description
```

---

# 9. Spacing System

Use a consistent spacing system.

```text
4px   → Very small spacing
8px   → Small spacing
16px  → Normal spacing
24px  → Section spacing
32px  → Large spacing
48px  → Major section spacing
```

This prevents the interface from appearing inconsistent.

---

# 10. Reusable UI Components

The React application should use reusable components.

```text
components/
│
├── Navbar
├── Sidebar
├── Button
├── Input
├── Select
├── SearchBar
├── Filter
├── EventCard
├── EventStatus
├── SeatAvailability
├── Modal
├── Toast
├── LoadingSpinner
├── Pagination
├── QRCode
├── DataTable
├── Badge
├── EmptyState
└── ConfirmDialog
```

---

# 11. Landing Page

## Purpose

Introduce CampusConnect and encourage students to discover events.

### Layout

```text
┌───────────────────────────────────────────────┐
│ CampusConnect       Events  Login  Register   │
├───────────────────────────────────────────────┤
│                                               │
│       Discover. Register. Participate.        │
│                                               │
│  Discover workshops, symposiums and           │
│  college events in one place.                 │
│                                               │
│       [Explore Events]  [Get Started]         │
│                                               │
├───────────────────────────────────────────────┤
│                                               │
│              Upcoming Events                  │
│                                               │
│   [Event]       [Event]       [Event]         │
│                                               │
└───────────────────────────────────────────────┘
```

---

# 12. Login Page

## UI Elements

* Email
* Password
* Show/Hide password
* Login button
* Forgot password
* Register link

### Flow

```text
Email
  ↓
Password
  ↓
[Login]
  ↓
Authentication
  ↓
Role Detection
  ↓
Dashboard
```

---

# 13. Registration Page

## Fields

* Full Name
* Email
* Student ID
* Department
* Password
* Confirm Password

### UX Requirements

* Required fields clearly marked.
* Password requirements displayed.
* Inline validation.
* Password confirmation.
* Clear error messages.

Example:

```text
Password
[••••••••••]

✓ At least 8 characters
✓ Contains a number
✓ Contains uppercase letter
```

---

# 14. Student Dashboard

The dashboard should immediately show relevant events.

```text
┌──────────────────────────────────────────────┐
│ Good Morning, Student 👋                     │
│ Find your next learning experience.          │
│                                              │
│ [ 🔍 Search events... ]                      │
│                                              │
│ Categories:                                  │
│ [All] [Workshop] [Symposium] [Technical]     │
│                                              │
│ Upcoming Events                              │
│                                              │
│ ┌───────────┐ ┌───────────┐ ┌───────────┐   │
│ │ Event     │ │ Event     │ │ Event     │   │
│ │ Details   │ │ Details   │ │ Details   │   │
│ │ [View]    │ │ [View]    │ │ [View]    │   │
│ └───────────┘ └───────────┘ └───────────┘   │
└──────────────────────────────────────────────┘
```

---

# 15. Event Card Design

The Event Card is one of the most important reusable components.

```text
┌──────────────────────────────────┐
│ [Workshop]                       │
│                                  │
│ AI & Machine Learning Workshop   │
│                                  │
│ 📅 12 Oct 2026                   │
│ 🕐 10:00 AM                      │
│ 📍 Seminar Hall                  │
│                                  │
│ 120 / 200 seats available        │
│                                  │
│ [View Details]                   │
└──────────────────────────────────┘
```

### Event Card should display

* Category badge
* Event name
* Date
* Time
* Venue
* Seat availability
* Registration status
* CTA button

---

# 16. Event Details Page

```text
┌─────────────────────────────────────────────┐
│ ← Back to Events                            │
│                                             │
│ AI & Machine Learning Workshop              │
│                                             │
│ Workshop                                    │
│                                             │
│ 📅 12 October 2026                          │
│ 🕐 10:00 AM – 1:00 PM                       │
│ 📍 Seminar Hall                             │
│                                             │
│ About this Event                            │
│ ─────────────────────────────────────────── │
│ Learn fundamentals of machine learning...   │
│                                             │
│ Organizer                                   │
│ Computer Science Club                       │
│                                             │
│ Seats                                       │
│ ████████████░░░░ 120 / 200                  │
│                                             │
│ Registration closes: 10 October             │
│                                             │
│              [ Register Now ]               │
└─────────────────────────────────────────────┘
```

---

# 17. Seat Availability UX

Seat availability should be visually clear.

### Available

```text
120 / 200 Seats Available
████████████░░░░░░
```

### Almost Full

```text
15 / 200 Seats Available
██████████████████░
⚠ Almost Full
```

### Full

```text
0 / 200 Seats Available

[Registration Closed]
```

The registration button should be disabled when the event is full.

---

# 18. Registration Confirmation

After successful registration, show a confirmation screen or modal.

```text
┌──────────────────────────────────┐
│          ✓                       │
│                                  │
│ Registration Successful!         │
│                                  │
│ AI & Machine Learning Workshop   │
│                                  │
│ Your registration is confirmed.  │
│                                  │
│ [View QR Pass]                   │
│ [My Registrations]               │
└──────────────────────────────────┘
```

---

# 19. My Registrations Page

```text
My Registrations

[Upcoming] [Completed] [Cancelled]

┌────────────────────────────────────┐
│ AI Workshop                        │
│ 12 Oct 2026                        │
│ Registration: Confirmed            │
│                                    │
│ [View Pass] [Cancel]               │
└────────────────────────────────────┘
```

---

# 20. QR Pass UI

The QR Pass should contain:

* Student name
* Student ID
* Event name
* Date
* Venue
* Registration ID
* QR code

```text
┌──────────────────────────────────┐
│          CAMPUSCONNECT            │
│                                  │
│ AI & ML WORKSHOP                 │
│                                  │
│ Student: John                    │
│ Student ID: CS2026001            │
│ Date: 12 Oct 2026                │
│ Venue: Seminar Hall              │
│                                  │
│             ┌──────┐             │
│             │ QR   │             │
│             │ CODE │             │
│             └──────┘             │
│                                  │
│ Registration Confirmed ✓         │
└──────────────────────────────────┘
```

---

# 21. Organizer Dashboard

Organizer dashboard should focus on event management.

```text
┌──────────────────────────────────────────────┐
│ Organizer Dashboard                          │
│                                              │
│ ┌─────────┐ ┌─────────┐ ┌─────────┐         │
│ │ Events  │ │Registr. │ │Attendance│         │
│ │   12    │ │   430   │ │   350    │         │
│ └─────────┘ └─────────┘ └─────────┘         │
│                                              │
│ [ + Create Event ]                           │
│                                              │
│ My Events                                    │
│                                              │
│ Event Name       Status       Registrations  │
│ AI Workshop      Approved         120        │
│ Hackathon        Pending           80        │
│ Seminar          Approved         200        │
└──────────────────────────────────────────────┘
```

---

# 22. Create Event UI

Use a structured form rather than placing every field together.

### Section 1 – Basic Information

* Event Name
* Description
* Category

### Section 2 – Event Schedule

* Date
* Start Time
* End Time
* Registration Deadline

### Section 3 – Location

* Venue
* Department

### Section 4 – Capacity

* Maximum Participants

### Section 5 – Submit

```text
[Cancel]          [Save Draft] [Submit for Approval]
```

---

# 23. Admin Dashboard

The Admin dashboard provides a system-wide overview.

```text
┌──────────────────────────────────────────────┐
│ Admin Dashboard                              │
│                                              │
│ Users       Events      Registrations        │
│ 2,450       128         8,420                │
│                                              │
│ Pending Event Approvals: 8                  │
│                                              │
│ ┌──────────────────────────────────────────┐ │
│ │ Event             Organizer    Action    │ │
│ │ AI Workshop       CSE Club     [Review]  │ │
│ │ Robotics Seminar  Robotics     [Review]  │ │
│ └──────────────────────────────────────────┘ │
└──────────────────────────────────────────────┘
```

---

# 24. Admin Event Approval UI

```text
┌──────────────────────────────────────────────┐
│ Event Approval                               │
│                                              │
│ AI & Machine Learning Workshop               │
│                                              │
│ Organizer: Computer Science Club             │
│ Date: 12 October 2026                        │
│ Venue: Seminar Hall                          │
│ Capacity: 200                                │
│                                              │
│ Description                                  │
│ ───────────────────────────────────────────  │
│ Event description...                         │
│                                              │
│ [Reject]                    [Approve Event]  │
└──────────────────────────────────────────────┘
```

---

# 25. Attendee Management UI

```text
Attendees – AI Workshop

Search: [ 🔍 Search student ]

┌─────────────────────────────────────────────┐
│ Student ID │ Name       │ Dept │ Status     │
├────────────┼────────────┼──────┼────────────┤
│ CS001      │ Arun       │ CSE  │ Registered │
│ CS002      │ Kumar      │ IT   │ Attended   │
│ CS003      │ Priya      │ CSE  │ Registered │
└─────────────────────────────────────────────┘

[Export CSV]    [Export JSON]
```

---

# 26. Attendance UI

Attendance should provide a simple scanning workflow.

```text
┌───────────────────────────────────┐
│         Event Attendance          │
│                                   │
│       ┌─────────────────┐         │
│       │                 │         │
│       │    QR SCANNER   │         │
│       │                 │         │
│       └─────────────────┘         │
│                                   │
│ Scan student's QR pass            │
│                                   │
│ ✓ 120 Students Checked In         │
│ ○ 80 Students Remaining           │
└───────────────────────────────────┘
```

---

# 27. Search and Filter UX

Search and filters should be easy to access.

```text
┌─────────────────────────────────────────────┐
│ 🔍 Search events...                         │
├─────────────────────────────────────────────┤
│ Category ▼   Department ▼   Date ▼          │
│ Availability ▼                              │
└─────────────────────────────────────────────┘
```

On mobile, filters can open in a modal or bottom sheet.

---

# 28. Notifications UX

Notifications should use a notification panel.

```text
🔔 Notifications

✓ Registration confirmed
  AI Workshop

⚠ Event starts tomorrow
  Robotics Seminar

✓ Certificate available
  Web Development Workshop
```

Unread notifications should have a visual indicator.

---

# 29. Modal vs Sidebar Usage

## Modal

Use modals for short, focused actions.

Examples:

* Confirm cancellation
* Confirm event deletion
* Quick event preview
* Approval confirmation

Example:

```text
┌──────────────────────────────┐
│ Cancel Registration?         │
│                              │
│ Are you sure you want to     │
│ cancel this registration?    │
│                              │
│ [Keep Registration] [Cancel] │
└──────────────────────────────┘
```

## Sidebar

Use sidebars for persistent navigation or filters.

Examples:

* Dashboard navigation
* Event filters
* Admin navigation

---

# 30. Loading States

The application should display loading feedback while API requests are running.

Example:

```text
Loading Events...
      ⟳
```

For event cards, skeleton loading can be used:

```text
┌──────────────────────┐
│ ███████████████      │
│ ██████████           │
│ ████████             │
│ █████████████        │
└──────────────────────┘
```

---

# 31. Empty States

When there is no data, show a useful message.

### No Events

```text
        📅

No events found.

Try changing your search or filters.
```

### No Registrations

```text
        🎫

You haven't registered for any events yet.

[Explore Events]
```

### No Attendees

```text
        👥

No students have registered yet.
```

---

# 32. Error States

Errors should be understandable to normal users.

Avoid:

```text
Error 500
```

Prefer:

```text
Something went wrong.

We couldn't load the events.
Please try again.

[Retry]
```

---

# 33. Toast Notifications

Use toast notifications for short-lived feedback.

### Success

```text
✓ Event created successfully
```

### Error

```text
✕ Registration failed
```

### Warning

```text
⚠ Only 5 seats remaining
```

---

# 34. Responsive Design

The application must support:

```text
Desktop
   ↓
Tablet
   ↓
Mobile
```

### Desktop

```text
Navbar
────────────────────────────
Sidebar │ Main Content
        │
        │ Event Cards
```

### Mobile

```text
┌───────────────────┐
│ ☰ CampusConnect 🔔│
├───────────────────┤
│ Search            │
├───────────────────┤
│ Event Card         │
├───────────────────┤
│ Event Card         │
├───────────────────┤
│ Event Card         │
└───────────────────┘
```

---

# 35. Mobile Navigation

Use a bottom navigation bar for frequently used student features.

```text
┌─────────────────────────────────┐
│                                 │
│          Main Content           │
│                                 │
├─────────────────────────────────┤
│ 🏠      🔍      🎫      👤      │
│ Home   Events   Tickets Profile │
└─────────────────────────────────┘
```

---

# 36. Accessibility

The application should follow basic accessibility principles.

### Requirements

* Sufficient text/background contrast.
* Keyboard navigation.
* Visible focus states.
* Labels for form fields.
* Meaningful button names.
* Alternative text for meaningful images.
* Do not rely only on color to communicate status.
* Accessible error messages.

Example:

Instead of only:

```text
🔴
```

Use:

```text
🔴 Event Full
```

---

# 37. Form UX

Forms should follow a predictable structure.

```text
Label
Input
Helper Text
Error Message
```

Example:

```text
Event Name *

[________________________]

Enter the official name of the event.

⚠ Event name is required.
```

---

# 38. Confirmation UX

Destructive actions should require confirmation.

Actions requiring confirmation:

* Cancel registration
* Delete event
* Reject event
* Remove attendee

Example:

```text
Delete Event?

This action cannot be undone.

[Cancel] [Delete Event]
```

---

# 39. Dashboard UX Principles

Each dashboard should answer three questions immediately:

### Student

```text
What events can I attend?
What have I registered for?
What is happening soon?
```

### Organizer

```text
How are my events performing?
How many students registered?
What needs my attention?
```

### Admin

```text
What needs approval?
How is the platform performing?
What needs administrative attention?
```

---

# 40. UX User Journey – Student

```text
Landing Page
      ↓
Register / Login
      ↓
Student Dashboard
      ↓
Browse Events
      ↓
Search / Filter
      ↓
Event Details
      ↓
Register
      ↓
Confirmation
      ↓
QR Pass
      ↓
Attend Event
      ↓
QR Check-in
      ↓
Feedback
      ↓
Certificate
```

---

# 41. UX User Journey – Organizer

```text
Login
  ↓
Organizer Dashboard
  ↓
Create Event
  ↓
Submit
  ↓
Admin Approval
  ↓
Event Published
  ↓
Student Registrations
  ↓
Manage Attendees
  ↓
QR Attendance
  ↓
Export Data
  ↓
Analytics
```

---

# 42. UX User Journey – Admin

```text
Login
  ↓
Admin Dashboard
  ↓
Review Pending Events
  ↓
View Event Details
  ↓
Approve / Reject
  ↓
Monitor Users
  ↓
Monitor Registrations
  ↓
View Analytics
  ↓
Audit Logs
```

---

# 43. Frontend Component Architecture

```text
src/
│
├── components/
│   ├── Navbar.jsx
│   ├── Sidebar.jsx
│   ├── Button.jsx
│   ├── Input.jsx
│   ├── EventCard.jsx
│   ├── SearchBar.jsx
│   ├── Filter.jsx
│   ├── Modal.jsx
│   ├── Toast.jsx
│   ├── QRCode.jsx
│   └── DataTable.jsx
│
├── pages/
│   ├── Home.jsx
│   ├── Login.jsx
│   ├── Register.jsx
│   ├── Events.jsx
│   ├── EventDetails.jsx
│   ├── MyRegistrations.jsx
│   ├── StudentDashboard.jsx
│   ├── OrganizerDashboard.jsx
│   ├── AdminDashboard.jsx
│   ├── CreateEvent.jsx
│   ├── Attendees.jsx
│   └── Analytics.jsx
│
├── services/
│   ├── authService.js
│   ├── eventService.js
│   ├── registrationService.js
│   └── adminService.js
│
├── context/
│   └── AuthContext.jsx
│
├── routes/
│   └── AppRoutes.jsx
│
└── utils/
    └── validation.js
```

---

# 44. UI State Management

The frontend should manage states such as:

```text
Authentication State
        │
        ├── Logged In
        └── Logged Out

Event State
        │
        ├── Loading
        ├── Loaded
        └── Error

Registration State
        │
        ├── Available
        ├── Registering
        ├── Registered
        └── Failed
```

---

# 45. Design Tokens

Centralize UI values.

```text
colors
spacing
font sizes
border radius
shadows
breakpoints
```

Example conceptual structure:

```javascript
const theme = {
  spacing: {
    sm: "8px",
    md: "16px",
    lg: "24px"
  },

  radius: {
    sm: "6px",
    md: "10px",
    lg: "16px"
  }
};
```

---

# 46. API and UI Interaction

```text
User Action
     │
     ▼
React Component
     │
     ▼
Service Function
     │
     ▼
Axios
     │
     ▼
Spring Boot REST API
     │
     ▼
MongoDB
     │
     ▼
JSON Response
     │
     ▼
React State Update
     │
     ▼
UI Update
```

Example:

```text
Click Register
      ↓
POST /api/events/{eventId}/register
      ↓
Backend validation
      ↓
Registration saved
      ↓
JSON success response
      ↓
Show "Registration Successful"
```

---

# 47. Design Success Criteria

The UI/UX implementation should achieve:

* Students can find an event quickly.
* Event information is easy to understand.
* Registration requires minimal steps.
* Seat availability is clearly visible.
* Users understand system feedback.
* Organizers can manage attendees easily.
* Admins can review events efficiently.
* The application works on mobile and desktop.
* Navigation remains consistent across roles.
* Important actions are protected against accidental mistakes.

---

# 48. Final UI/UX Flow

```text
                    CAMPUSCONNECT
                          │
                          ▼
                    Landing Page
                          │
                    Login / Register
                          │
                          ▼
                  Role Identification
                          │
          ┌───────────────┼───────────────┐
          │               │               │
          ▼               ▼               ▼
       STUDENT         ORGANIZER         ADMIN
          │               │               │
          ▼               ▼               ▼
    Browse Events     Create Event    Review Events
          │               │               │
          ▼               ▼               ▼
    Search / Filter   Submit Event    Approve / Reject
          │               │               │
          ▼               ▼               │
    Event Details     Published Event    │
          │               │               │
          ▼               ▼               │
      Register       Manage Attendees     │
          │               │               │
          ▼               ▼               │
       QR Pass        Attendance          │
          │               │               │
          └───────────────┼───────────────┘
                          ▼
                     Analytics
                          │
                          ▼
                    Reports / Data
```

---

# 49. Final Design Principle

**CampusConnect UI/UX should follow a simple principle:**

> **Discover → Understand → Register → Participate → Track**

The interface should keep the student's primary task—**finding and registering for college events**—simple, while providing organizers and administrators with powerful management interfaces.
