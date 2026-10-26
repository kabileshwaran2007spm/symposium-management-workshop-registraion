# CampusConnect

College Symposium & Workshop Management System — a full-stack web application where
students discover and register for campus events, organizers publish and run them,
and administrators oversee the whole platform.

## Tech stack

| Layer     | Technology                                             |
|-----------|--------------------------------------------------------|
| Frontend  | React 19, Vite 8, React Router 7, Axios, qrcode.react  |
| Backend   | Spring Boot 4.1.2 (Java 25), Spring Security, JOSE JWT |
| Database  | MongoDB 9 (Spring Data MongoDB)                        |
| Build     | Maven 3.9.16 via Maven Wrapper, Node.js 24 LTS, Vite 8  |

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
- Java 25 (JDK)
- Node.js 24 LTS (npm 11 or newer)
- MongoDB 9 running on `localhost:27017`

### 1. Database

Ensure MongoDB is running. The `campusconnect` database is created automatically.
Optional demo seeding is disabled by default to preserve existing data (see below).

### 2. Backend

```powershell
Set-Location backend
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Create an unpredictable HS256 secret for this PowerShell session; never print or commit it.
$bytes = New-Object byte[] 48
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
try {
  $rng.GetBytes($bytes)
  $env:JWT_SECRET = [Convert]::ToBase64String($bytes)
} finally {
  $rng.Dispose()
}

.\mvnw.cmd -B test
.\mvnw.cmd spring-boot:run
```

The API listens on `http://localhost:8080`. See `backend/.env.example` for all
environment variable names. Spring Boot does not load `.env` files automatically;
set variables in the PowerShell session that starts the backend. MongoDB defaults
to `127.0.0.1:27017/campusconnect`. Seeding is disabled by default to preserve
existing data; to seed an empty database, set `APP_SEED_ENABLED=true` and choose a
private `APP_SEED_PASSWORD` of at least 8 characters in that session.

### 3. Frontend

```powershell
Set-Location frontend
if ($env:NODE_HOME) { $env:PATH = "$env:NODE_HOME;$env:PATH" }
npm.cmd install
npm.cmd run dev
```

Open `http://localhost:5173`.

### Run the full stack with Docker

Install Docker Desktop and make sure its engine is running. From the repository
root, create a local JWT secret and start the services:

```powershell
$bytes = New-Object byte[] 48
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
if (-not (Test-Path '.env')) {
  try {
    $rng.GetBytes($bytes)
    [IO.File]::WriteAllText('.env', 'JWT_SECRET=' + [Convert]::ToBase64String($bytes))
  } finally {
    $rng.Dispose()
  }
} else {
  $rng.Dispose()
  Write-Output 'Preserving existing .env; ensure it contains a secure JWT_SECRET.'
}

docker compose up --build
```

Open `http://localhost:3000`. The frontend proxies API requests to the backend;
the backend connects to the MongoDB service over the private Compose network.
MongoDB data is stored in the persistent `mongodb-data` volume. Seeding is off
by default. To stop the services, run `docker compose down`; this preserves the
database volume. To remove the database as well, run `docker compose down -v`.

### Deploy the backend on Render

Create a **Web Service** from this repository with these settings:

| Render field | Value |
|---|---|
| Name | Any available service name |
| Branch | `main` |
| Root Directory | `backend` |
| Language | `Docker` |
| Dockerfile Path | `./Dockerfile` |
| Docker Build Context Directory | `.` |
| Build Command | Leave blank |
| Start Command | Leave blank |
| Health Check Path | `/api/events` |

Render provides `PORT` automatically; the backend uses it and defaults to
`8080` for local development. Add these environment variables in Render:

| Variable | Value |
|---|---|
| `MONGODB_URI` | Your MongoDB Atlas connection string for the `campusconnect` database |
| `JWT_SECRET` | A unique, randomly generated secret of at least 32 characters |
| `CORS_ALLOWED_ORIGINS` | The exact public Vercel frontend origin, such as `https://your-app.vercel.app` |
| `APP_SEED_ENABLED` | `false` |

Do not commit the MongoDB URI or JWT secret. In MongoDB Atlas, allow network
connections from the Render service; prefer Render's outbound IP ranges where
available rather than unrestricted access.

For the Vercel frontend, set `VITE_API_BASE_URL` to the Render backend API URL,
for example `https://your-backend.onrender.com/api`, then redeploy the frontend.
The Vercel frontend origin entered in `CORS_ALLOWED_ORIGINS` must match the
browser-visible origin exactly.

### 4. Optional demo data

Seeding is disabled by default. To seed an empty database, set
`APP_SEED_ENABLED=true` and provide a private `APP_SEED_PASSWORD` in the same
PowerShell session before starting the backend. Seeding is skipped when any user
already exists, so it does not overwrite existing data. The seeded accounts use
that locally configured password:

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
  src/test/          102 unit + integration tests
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

## Scope: implemented vs. deferred

This build covers the complete MVP described in `plan/`. The items below are
specified in the planning documents as future phases and are **not** part of the
current system — they are listed so the docs and the code agree:

| Feature | Status |
|---------|--------|
| Notifications (registration, approval, reminders) | Deferred — `plan/implementation.md` §41 |
| Feedback collection and viewing | Deferred — PRD §6 |
| Certificates | Deferred — `plan/implementation.md` §41 |
| Audit logs | Deferred — `plan/implementation.md` §42 |
| Waitlist auto-promotion | Partial — `WAITLISTED` status and analytics exist; when an event is full, registration returns 409 rather than queueing |
| Club management (create/edit clubs) | Read-only — the admin club directory lists seeded clubs |

Everything else in the planning documents — authentication, roles, event lifecycle,
registration, QR attendance, exports, analytics and the admin console — is
implemented and covered by the test suite.

## Verification

- Backend: `./mvnw -B test` — **102 tests**, all passing
  - Unit: Auth 7, Registration 19, Event 21, Attendance 9, Admin 8, Jwt 10, Csv 8
  - Integration: `ApiAuthorizationIntegrationTest` 14 (role matrix, 401/403 boundaries,
    password-hash non-disclosure) and `EventLifecycleIntegrationTest` 5 (the full
    documented flow: create → hidden → approve → public → register → duplicate 409 →
    cancel → seat release → re-register → full 409 → stale-QR refused → check-in →
    analytics; plus rejection, resubmission, CSV export and envelope contracts)
- API: 66-check end-to-end smoke script (auth, concurrency, permissions, transitions,
  exports, QR check-in)
- Frontend: `npm run build` (Vite production build) and manual browser E2E across all
  three roles
