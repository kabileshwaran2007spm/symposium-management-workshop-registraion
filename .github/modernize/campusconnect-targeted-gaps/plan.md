# Modernization Plan: CampusConnect Targeted Gap Closure

**Project**: CampusConnect

## Technical Framework

- **Language**: Java 25 (backend) and JavaScript (frontend)
- **Framework**: Spring Boot 4.1.2, React 19, Vite 8
- **Build Tool**: Maven and npm
- **Database**: MongoDB
- **Architecture constraint**: Preserve the existing React/Vite + Spring Boot/MongoDB stack. No framework, runtime, database, or cloud migration is in scope.

## Overview

This plan is limited to the small set of confirmed reliability gaps in the current
working tree. It preserves all existing user changes, including the password-reset
implementation, and verifies the bearer-token CORS behavior and the one known
password-reset integration-test failure. It does not rebuild existing product
features or treat assessment findings as proof of a production defect.

Verified evidence supplied for this plan:

- Render `GET /api/events` returns HTTP 200 with a valid, empty paginated envelope.
- The actual Vercel Origin passes CORS `GET` and `OPTIONS`; an untrusted preflight
  is rejected with HTTP 403.
- `VITE_API_BASE_URL` already targets the Render `/api` endpoint.
- The Vercel deployment URL redirects to SSO; this is an external access/protection
  blocker, not a demonstrated API or code defect.
- The focused CORS changes are already present in the working tree, and frontend
  lint/build plus `SecurityConfigCorsTest` have passed.
- The previous backend suite ran 108 tests with one failure: the reset-endpoint
  integration test expected HTTP 400 while the current business-rule response is
  HTTP 409. This must be classified against the intended API contract, not
  automatically “fixed” by changing either the implementation or assertion.

The assessment report contains Azure-readiness findings, including localhost,
URL, network-protocol, and credential-related rules. Only findings that directly
overlap with the verified API/CORS behavior are in scope for reconciliation.
Intentional local development/test configuration is not a production defect.
Credential values are not to be inspected or reported. Azure recommendations,
runtime upgrades, dependency upgrades, and other assessment findings are excluded
unless a concrete in-scope regression is demonstrated.

## Scope and Guardrails

- Preserve every pre-existing staged, unstaged, and untracked user change.
- In particular, do not replace, revert, overwrite, or broadly refactor the
  password-reset code or unrelated user work.
- Keep bearer-token authentication stateless. Do not enable browser-managed
  credentials unless a separately verified requirement establishes their need.
- Do not change the known-good API base URL or loosen CORS to wildcard/untrusted
  origins.
- Do not inspect, print, copy, or modify secrets or credentials.
- Do not connect tests to, mutate, or clean production data. Before running any
  integration test that persists/deletes data, verify its test profile points to
  an isolated test database; skip it if isolation cannot be established without
  inspecting credentials.
- No deployment, production-data operation, commit, push, discard, or revert is
  authorized by this plan.

## Implementation and Verification Approach

1. Review the current diff before editing; preserve unrelated work and identify
   whether the focused CORS fix is already complete.
2. Reconcile only the CORS/connectivity findings supported by the supplied live
   checks. Avoid repeating or broadening changes that already pass.
3. Investigate the 400-versus-409 test failure by tracing the reset endpoint,
   exception mapping, and validation/business-rule semantics. Make the smallest
   contract-consistent correction, or leave behavior unchanged and document why
   the expected 409 is valid.
4. Run focused frontend lint/build and backend CORS/reset tests, followed by the
   backend suite if test database isolation is confirmed. Review the final diff
   and report exactly which files changed and which checks ran.
5. Report the Vercel SSO redirect as an external manual action: the deployment
   owner must adjust access/protection if public access is intended, then verify
   the actual hosted page and browser-to-Render flow. Do not claim deployment or
   production verification from local tests.

## Migration Impact Summary

| Application | Original Service | New Azure Service | Authentication | Comments |
|-------------|------------------|-------------------|----------------|----------|
| CampusConnect | React/Vite + Spring Boot/MongoDB | None | Existing bearer token | Targeted code/test gap closure only |

## Open Questions & Questionnaire

No unanswered implementation choices are needed for this narrow scope. No
infrastructure provisioning, Azure deployment, or separate integration-test
generation task is included. Existing tests are run only when their data target
is confirmed isolated from production.

## Completion Criteria

- The existing allowed-origin CORS behavior remains restricted and successful
  for the verified Vercel origin, while untrusted origins remain rejected.
- The SPA's network error message does not assert that CORS is the cause when
  the request has no HTTP response.
- The password-reset integration-test mismatch is resolved only in accordance
  with the established API contract; the outcome and reasoning are recorded.
- No unrelated user changes are modified, no production data is touched, and no
  secrets or credentials are inspected or exposed.
- Focused checks and any safely runnable broader suite are reported accurately,
  along with changed files and the external Vercel SSO follow-up.
