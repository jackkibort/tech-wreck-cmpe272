# /frontend: SecureHR portal (owner: Arthi)

React 19 single-page app for the employee portal. Users sign in once through Keycloak (OIDC
authorization code + PKCE) and then search the directory, open profiles with title and salary
history, view the org chart, use the document repository and, for HR Admins, export CSV.
It is built against `api-spec/employee-api.yaml` and Section 10 of the plan for documents.

**The UI hides controls by role for usability only.** Every request is authorized by the APIs
(Sections 8.2 and 10.2), and every page shows only the fields the API returned.

## Contents

| Path                    | Purpose                                                                    |
| ----------------------- | -------------------------------------------------------------------------- |
| `src/auth/`             | OIDC settings, sign-in gate, session (from `GET /api/me`), role helpers    |
| `src/shared/api/`       | The single API client (tokens, request IDs, errors) and API types          |
| `src/shared/`           | Layout, error/loading/pagination components, data-loading hook, formatting |
| `src/employees/`        | Directory search, employee detail, title history, salary chart             |
| `src/org/`              | Org chart (management chain)                                               |
| `src/export/`           | HR Admin CSV export                                                        |
| `src/documents/`        | Document list, download, upload and delete                                 |
| `src/test/`             | Test setup, fake API and render helpers                                    |
| `Dockerfile`, `docker/` | `securehr/ui` image: unprivileged NGINX on port 8080                       |
| `compose.yaml`          | Area file: the `ui` service                                                |

## Pages

| Route               | Page                                                                   | Who sees it                    |
| ------------------- | ---------------------------------------------------------------------- | ------------------------------ |
| `/directory`        | Search by name prefix, department, title, hire-date range; 25 per page | EMPLOYEE, MANAGER, HR_ADMIN    |
| `/employees/:empNo` | Detail, title history; salary chart for self and HR Admin              | Per Section 8.2 (API decides)  |
| `/org/:empNo`       | Management chain up to the department manager                          | EMPLOYEE, MANAGER, HR_ADMIN    |
| `/documents`        | List and download; upload and delete for HR Admin                      | Per Section 10.2 (API decides) |
| `/export`           | CSV export of the chosen filters (max 10,000 rows)                     | HR_ADMIN                       |

A user whose `GET /api/me` returns 403 (an Auditor, or an unlinked GitHub account) sees a
"No access" page with a sign-out button.

## Security

| Rule                                | How                                                                                                                                                                                                                                                                   |
| ----------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Tokens in memory only (Section 4.3) | `oidc-client-ts` user store is `InMemoryWebStorage`; a reload signs in again silently through the Keycloak SSO session. Only the PKCE state for the redirect itself sits in sessionStorage until the callback, and it holds no token. Tested in `oidcConfig.test.ts`. |
| PKCE                                | `response_type: code`; oidc-client-ts always uses S256                                                                                                                                                                                                                |
| No iframes                          | Refresh-token renewal and `monitorSession: false`, so the edge CSP can set `frame-src 'none'`                                                                                                                                                                         |
| Cookies never sent to the APIs      | `credentials: 'omit'`; the token travels only in `Authorization`                                                                                                                                                                                                      |
| Salary reads only when allowed      | The salary request is made only for self or HR Admin, so the UI never triggers needless `ACCESS_DENIED` audit events                                                                                                                                                  |
| No runtime secrets                  | `/config.json` holds only the public issuer URL and client ID                                                                                                                                                                                                         |

## Error handling (TC-UI-02)

All requests go through `ApiClient`, which turns any non-2xx response into an `ApiError`
carrying the status, the API's message and the request ID. `ErrorMessage` then shows:

| Status  | Shown                                                                                 |
| ------- | ------------------------------------------------------------------------------------- |
| 401     | "Your session has expired. Signing you in again…" and the client starts a new sign-in |
| 403     | "Access denied" with the API's message                                                |
| 429     | "Too many requests… wait a few seconds" with a **Try again** button                   |
| 0 / 5xx | Connection or server problem, with **Try again**                                      |

The request ID is shown so a user can quote it and an Auditor can find the event in Grafana.

## Configuration

The same image runs in production and staging. At start-up the container writes
`/config.json` from two Section 0.9 variables:

| Variable              | Example                                       |
| --------------------- | --------------------------------------------- |
| `KEYCLOAK_ISSUER_URI` | `https://auth.securehr.local/realms/securehr` |
| `PORTAL_CLIENT_ID`    | `securehr-portal`                             |

The redirect and post-logout URIs are the page's own origin (`https://securehr.local/`,
`https://staging.securehr.local:8443/`), matching the Keycloak client (Section 0.5).

## How to run

Node.js 22 LTS (>= 22.12).

```bash
cd frontend
npm ci
npm run dev          # Vite dev server on http://localhost:5173 (uses public/config.json)
```

For a real sign-in the app must be served from `https://securehr.local`, since that is the
redirect URI registered in Keycloak: run the full stack (root README) and open the portal there.

Container only:

```bash
docker compose -p securehr-prod --env-file .env -f frontend/compose.yaml up -d --build
```

## How to test

| Check                                   | Command                 |
| --------------------------------------- | ----------------------- |
| Unit and component tests (Vitest)       | `npm test`              |
| Coverage report (`coverage/index.html`) | `npm run test:coverage` |
| Lint (ESLint, zero warnings)            | `npm run lint`          |
| Format (Prettier)                       | `npm run format:check`  |
| Type check (strict)                     | `npm run typecheck`     |

Jenkins runs the same commands (Section 11.2, stages 3 and 7). Test names carry their catalogue
IDs:

| ID       | Tests                                                                                                                              |
| -------- | ---------------------------------------------------------------------------------------------------------------------------------- |
| TC-UI-01 | `Layout.test.tsx` (export link), `EmployeePage.test.tsx` (salary), `ExportPage.test.tsx`, `DocumentsPage.test.tsx` (upload/delete) |
| TC-UI-02 | `errors.test.ts`, `EmployeePage.test.tsx` (401/403/429), `ExportPage.test.tsx` (429), `DocumentsPage.test.tsx` (403)               |

## Design decisions and patterns

| Pattern / decision                                   | Where                                          | Why                                                                |
| ---------------------------------------------------- | ---------------------------------------------- | ------------------------------------------------------------------ |
| **Facade**                                           | `shared/api/client.ts`                         | One place for tokens, request IDs and error mapping (Section 12.3) |
| **Context providers** for session and API client     | `auth/session.ts`, `shared/api/ApiContext.tsx` | Pages stay testable: tests inject a session and a fake API         |
| Roles from `GET /api/me`, not decoded from the token | `auth/SessionGate.tsx`                         | The UI shows what the API derived from the validated token         |
| Filters in the URL                                   | `employees/filters.ts`                         | Searches can be bookmarked; Back works                             |
| Stale-response guard                                 | `shared/hooks/useApiResource.ts`               | Results for an old filter are discarded                            |
| Lazy-loaded chart                                    | `employees/EmployeePage.tsx`                   | Keeps the charting library out of the first page load              |
| Runtime `/config.json`                               | `docker/40-write-config.sh`                    | One image for staging and production                               |
| Read-only container, non-root NGINX                  | `Dockerfile`, `compose.yaml`                   | Least privilege                                                    |

## Pinned versions

| Component                           | Version                                         |
| ----------------------------------- | ----------------------------------------------- |
| Build image                         | `node:22.23.5-alpine3.24`                       |
| Runtime image                       | `nginxinc/nginx-unprivileged:1.31.7-alpine3.24` |
| React / React DOM                   | 19.3.1                                          |
| React Router                        | 8.5.3                                           |
| oidc-client-ts / react-oidc-context | 3.5.0 / 3.3.1                                   |
| Recharts                            | 3.11.1                                          |
| Vite / Vitest                       | 8.3.8 / 5.1.2                                   |
| TypeScript                          | 5.9.3 (strict)                                  |

All npm dependencies are pinned to exact versions in `package.json` and `package-lock.json`.

## Open items

- The document field names follow Section 10.1 of the plan. Check them against
  `api-spec/documents-api.yaml` once Jack publishes it.
