# CampusConnect feature and deployment audit

**Audit date:** 2026-10-26  
**Scope:** Frontend routes and user-facing feature surfaces, API coverage, security/data integrity, local quality checks, and the supplied production URLs.

## Summary

The requested frontend work and backend integrity fixes are implemented locally. Frontend tests, lint, and production build pass; all 117 backend tests pass. The production API is reachable and returns a valid empty page with the expected CORS headers. The supplied Vercel URL currently returns HTTP 404, so deployed UI navigation, Google sign-in, and browser behavior cannot be confirmed until the Vercel project/deployment URL is corrected.

## Feature status

| Area | Status | Evidence and limits |
|---|---|---|
| Public event discovery | Implemented; backend and UI build verified | Public API supports search/filter, pagination, and upcoming/latest/title sorting. Empty event pages are valid successful responses. |
| Event details and registration | Implemented; backend regression suite passes | Event detail, registration, QR pass, cancellation, and attendee/attendance flows are present. No end-to-end browser session was possible against the supplied 404 frontend URL. |
| Student dashboard | Implemented | Registration-derived totals, upcoming/completed/cancelled counts, and explicit registration loading/error states are displayed. |
| My activity | Implemented | Registration history supports event/registration-date sorting, event links, timestamps, and cancellation-aware QR pass visibility. |
| Profile | Implemented for supported fields | Authenticated users can edit name and department. Email, student ID, and role remain read-only; fields not represented by the backend are not presented as editable. |
| Settings | Implemented for supported account controls | Account information, password-reset link request, student activity link, and sign-out are available. Theme, notification, and other preference controls are not backed by this application and are not represented as working features. |
| Organizer and administrator surfaces | Present and role-gated | Event creation/editing, attendee and attendance management, review, users, registrations, clubs, and analytics routes exist. The full multi-role UI was not exercised end-to-end in a browser. |
| Google sign-in | Implemented in source; production configuration unverified | Frontend requires `VITE_GOOGLE_CLIENT_ID`; backend verifier uses `app.google.client-id` (commonly supplied as `APP_GOOGLE_CLIENT_ID`). A valid matching OAuth client and allowed origins are required. |
| API errors and JSON contract | Implemented; focused frontend tests pass | Network/timeout failures, HTTP failures, and invalid envelopes are represented separately; an empty event list is accepted. |
| Concurrent registration integrity | Implemented; backend tests pass | Seat claims are atomic; cancellation changes state conditionally before releasing a seat. |
| Event edit integrity | Implemented; backend tests pass | Editable fields are updated conditionally to avoid overwriting concurrent registration counts. |
| Password-reset session invalidation | Implemented; backend tests pass | Token versions are checked against current account state, and reset increments the version. |
| Direct-route refresh | Implemented in Vercel configuration; deployment unverified | The SPA rewrite is configured in `frontend/vercel.json`; the supplied deployment currently returns 404. |

## Route inventory

The route table contains 31 path declarations, including aliases, redirect routes, and the wildcard fallback.

| Path | Access | Surface |
|---|---|---|
| `/` | Public | Home |
| `/login` | Public | Email/password and optional Google sign-in |
| `/register` | Public | Student account registration |
| `/forgot-password` | Public | Request password reset |
| `/reset-password` | Public | Complete password reset |
| `/events` | Public | Event discovery |
| `/discover` | Public | Discovery alias |
| `/events/:id` | Public | Event details |
| `/profile` | Signed-in user | View/update supported profile fields |
| `/settings` | Signed-in user | Account security and session controls |
| `/dashboard` | Student | Student dashboard |
| `/my-registrations` | Student | Registration history |
| `/my-activity` | Student | Activity alias |
| `/my-registrations/:registrationId/pass` | Student | Registration QR pass |
| `/organizer` | Organizer or admin | Organizer dashboard |
| `/organizer/events/new` | Organizer or admin | Create event |
| `/organizer/events/:id/edit` | Organizer or admin | Edit event |
| `/organizer/events/:id/attendees` | Organizer or admin | Attendee list |
| `/organizer/events/:id/attendance` | Organizer or admin | Attendance management |
| `/organizer/events/:id/analytics` | Organizer or admin | Organizer analytics |
| `/admin` | Admin | Administrator dashboard |
| `/admin/events` | Admin | Event administration |
| `/admin/events/:id/review` | Admin | Event review |
| `/admin/users` | Admin | User administration |
| `/admin/registrations` | Admin | Registration administration |
| `/admin/clubs` | Admin | Club administration |
| `/admin/analytics` | Admin | Administrator analytics |
| `/login-success` | Public route with role redirect | Post-login redirect |
| `/unauthorized` | Public | Access-denied screen |
| `/home` | Redirect | Redirect to `/` |
| `*` | Public fallback | Not-found screen |

Route definitions and role guards are in `frontend/src/routes/AppRoutes.jsx`. Client-side guards improve navigation, while API authorization remains the security boundary.

## Production checks

Checked the supplied URLs directly on 2026-10-26:

- `GET https://symposium-management-workshop-registraion.onrender.com/api/events?size=3&page=0` returned **HTTP 200**, a valid success envelope, page `0`, size `3`, and **0 events**. This is an empty successful result, not a connection failure.
- The GET response included `Access-Control-Allow-Origin: https://symposium-management-workshop-registraion-lol3-94n7acls4.vercel.app`.
- An OPTIONS preflight for that origin, `GET`, and `authorization,content-type` returned **HTTP 200** with the expected origin, methods, and allowed headers.
- The supplied Vercel URL returned **HTTP 404**. Therefore the currently deployed React app, deep-link refreshes, and browser-originated requests from a live UI could not be verified. These HTTP checks confirm server headers, not a browser E2E session.
- No Vercel or Render settings were accessed or changed.

The frontend uses `VITE_API_BASE_URL`; production source also has a Render API fallback and normalizes the `/api` prefix exactly once. Set the Vercel production value to `https://symposium-management-workshop-registraion.onrender.com/api` for explicit configuration. The Vercel project must use the `frontend` directory as its root, install dependencies, build with `npm run build`, and publish `dist`. After correcting the Vercel project/domain or environment setting, create a **new production deployment** and verify that the deployed root and a direct route such as `/discover` return the application.

For Google sign-in, set the same Google OAuth client ID in Vercel as `VITE_GOOGLE_CLIENT_ID` and in Render as `APP_GOOGLE_CLIENT_ID` (Spring property `app.google.client-id`), and authorize the deployed frontend origin in the Google OAuth client. Do not put private credentials in frontend variables.

## Verification performed

| Check | Result |
|---|---|
| Frontend Node tests | Passed: 4/4 |
| Frontend lint | Passed |
| Frontend production build | Passed |
| Backend Maven tests | Passed: 117/117 |
| Public sorting integration regression | Passed |
| Production API GET/CORS response | HTTP 200; valid empty success page |
| Production API OPTIONS preflight | HTTP 200; expected CORS headers |
| Supplied production frontend URL | HTTP 404; deployment is blocked/unverified |

## Remaining actions and limits

1. Correct or confirm the active Vercel project/domain and redeploy from the frontend project; then run browser checks for login, Google sign-in, discovery, activity, profile, organizer, and admin flows.
2. Verify Vercel and Render OAuth client ID configuration in their dashboards; it was not accessible during this audit.
3. The live database currently returns no public events. The UI must display its empty state; no sample events were created and no database data was changed.
4. Notification preferences, theme preferences, and profile image/phone editing are not supported by the current data/API model. They remain unavailable rather than being shown as nonfunctional controls.
