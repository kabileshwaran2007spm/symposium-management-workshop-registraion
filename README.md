# CampusConnect

College Symposium & Workshop Management System — a full-stack web application where
students discover and register for campus events, organizers publish and run them,
and administrators oversee the whole platform.

## Tech stack

| Layer     | Technology                                             |
|-----------|--------------------------------------------------------|
| Frontend  | React 19, Vite 8, React Router 7, Axios, qrcode.react  |
| Backend   | Spring Boot 4.1.1 (Java 17), Spring Security, JOSE JWT |
| Database  | MongoDB 9 (Spring Data MongoDB)                        |
| Build     | Maven Wrapper (`mvnw`), Vite                           |

## Features

### Students
- Browse approved events with search, category/department/availability/date filters and pagination
- One-click registration with live seat availability (confirm or waitlist when full)
- Cancel registration (releases the seat for others, re-register allowed)
- "My registrations" with upcoming / completed / cancelled tabs
- QR pass per registration (encodes only an opaque token) with print support

### Organizers
- Dashboard of own events with status/category filters and per-event actions
- Create events (save as draft or submit for approval) and edit them (approved events stay approved; rejected ones resubmit)
- Attendee table with search and CSV export (includes cancelled registrations for a complete record)
- Attendance check-in: scan or paste a student's QR token, with a running "still expected" counter and CSV export
- Per-event analytics: capacity, registrations by status, attendance rate

### Administrators
- Platform dashboard with totals and the pending-approval queue
- Review screen to approve (publishes immediately) or reject events
- All-events table with filters, user directory with role filter, registration ledger, club directory
- Platform analytics with breakdowns by role, event status and registration status

### Platform
- JWT stateless auth (HS256), public signup always creates STUDENT accounts
- Role-based access at three layers: path matchers in `SecurityConfig`, `@PreAuthorize` on controllers, ownership checks in services
- Atomic seat claiming (`findAndModify` with `registeredCount < capacity`) — no overbooking under concurrency
- Partial unique index prevents duplicate active registrations per student per event
- Event lifecycle state machine (DRAFT → PENDING → APPROVED → COMPLETED/CANCELLED, REJECTED → PENDING)
- Consistent `{success, message, data}` envelope with typed error handling
- Delete-with-registrations becomes a soft cancel, preserving student QR passes

## Quick start

### Prerequisites
- Java 17 (JDK)
- Node.js 18+
- MongoDB 9 running on `localhost:27017`

### 1. Database

Ensure MongoDB is running. The `campusconnect` database is created automatically; demo data is seeded on first boot (see below).

### 2. Backend

```bash
cd backend
export JAVA_HOME="/path/to/jdk-17"    # Windows (Git Bash): /c/Program Files/Java/jdk-17
./mvnw -B test                        # run the 95-test suite
./mvnw -B package -DskipTests         # build the jar

JWT_SECRET="a-long-random-secret-at-least-32-chars" \
APP_SEED_ENABLED=true \
APP_SEED_PASSWORD=Campus@123 \
java -jar target/campusconnect-0.0.1-SNAPSHOT.jar
```

The API listens on `http://localhost:8080`. See `backend/.env.example` for all
environment variables (Mongo URI, CORS origins, seed toggle, JWT secret).

### 3. Frontend

```bash
cd frontend
npm install
cp .env.example .env.local            # adjust VITE_API_BASE_URL if needed
npm run dev
```

Open `http://localhost:5173`.

### 4. Demo accounts

Seeded automatically when the user collection is empty (password for all: `Campus@123`):

| Role      | Email                          |
|-----------|--------------------------------|
| Admin     | admin@campusconnect.edu        |
| Organizer | cse.club@campusconnect.edu     |
| Organizer | robotics.club@campusconnect.edu|
| Student   | arun@campusconnect.edu         |
| Student   | priya@campusconnect.edu        |
| Student   | rahul@campusconnect.edu        |
| Student   | sneha@campusconnect.edu        |
| Student   | vikram@campusconnect.edu       |

## API overview

Base URL `/api`. All JSON responses use the envelope `{success, message, data}`.

| Area        | Endpoints |
|-------------|-----------|
| Auth        | `POST /auth/register`, `POST /auth/login`, `POST /auth/logout` |
| Events      | `GET /events` (public, approved only), `GET /events/{id}`, `POST/PUT/DELETE /events…` (organizer) |
| Registration| `POST/DELETE /events/{id}/register` (student), `GET /users/me/registrations` |
| Organizer   | `GET /organizer/events`, `GET /organizer/events/{id}/attendees`, `…/analytics`, `…/export/csv`, `…/export/json` |
| Attendance  | `POST /attendance/check-in`, `GET /events/{id}/attendance`, `GET /events/{id}/attendance/export` |
| Admin       | `GET/PUT /admin/…` — users, events (+`/pending`, `/{id}/approve|reject`), registrations, clubs, analytics |

## Project layout

```
backend/
  src/main/java/com/campusconnect/
    config/          app wiring (Jackson, seed, CORS defaults)
    controller/      REST controllers (auth, events, organizer, attendance, admin)
    dto/             request/response records incl. ApiResponse envelope
    exception/       business-rule + global exception handling
    model/           MongoDB documents and enums
    repository/      Spring Data repositories (custom @Query where needed)
    security/        JWT filter/service, SecurityConfig, SecurityUtils
    service/         business logic (seat claiming, state machine, CSV)
  src/test/          95 unit + integration tests
frontend/
  src/
    components/      design-system widgets (Button, DataTable, Modal, badges…)
    context/         AuthContext (session + role helpers), ToastContext
    pages/           student + public pages
    pages/organizer/ organizer workspace
    pages/admin/     admin console
    routes/          AppRoutes.jsx — single route table with role guards
    services/        typed API clients over one Axios instance
    styles/          tokens/base/layout/components/pages CSS layers
    utils/           formatting, validation, useAsync data hook
```

## Verification

- Backend: `./mvnw -B test` — 95 tests, all passing
- API: 66-check end-to-end smoke script (auth, concurrency, permissions, transitions, exports, QR check-in)
- Frontend: `npm run build` (Vite production build) and manual browser E2E across all three roles
