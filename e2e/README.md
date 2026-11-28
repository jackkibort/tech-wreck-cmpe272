# /e2e: end-to-end tests (owner: Samhitha)

Playwright tests that drive the real portal, Keycloak, Grafana and Jenkins through the edge, the
way a person would (Section 13.2 of the implementation plan). Jenkins runs them against staging
in stage 12. Run them by hand against production before a demo.

Every test saves a screenshot at the end (Section 13.5), kept in the HTML report.

## Test cases

| ID | File | What it proves |
|---|---|---|
| TC-AUTH-01 | `01-auth.spec.ts` | The portal sends you to Keycloak; a directory login lands on the directory page |
| TC-AUTH-02 | `01-auth.spec.ts` | HR Admin must enter a one-time code |
| TC-AUTH-03 | `01-auth.spec.ts` | A dev user who signed in to Jenkins opens Grafana with no second login |
| TC-AUTH-04 | `01-auth.spec.ts` | After signing out of the portal, both the portal and Grafana ask for credentials again |
| TC-E2E-01 | `02-employee-journeys.spec.ts` | demo.employee searches by name, opens own profile, sees salary history |
| TC-E2E-02 | `02-employee-journeys.spec.ts` | demo.manager opens a team member: titles shown, no salary or birth date |
| TC-E2E-03 | `03-hr-operations.spec.ts` | demo.hradmin uploads a payslip for demo.employee, who then downloads it |
| TC-E2E-04 | `03-hr-operations.spec.ts` | demo.hradmin exports department d005 as CSV (header plus at most 10,000 rows) |
| TC-E2E-06 | `04-misuse.spec.ts` | After 5 wrong passwords, demo.employee2 can't sign in even with the right one |
| TC-E2E-07 | `04-misuse.spec.ts` | **Manual** (GitHub blocks automated sign-in); steps below |
| TC-SEC-02 | `05-security-headers.spec.ts` | HSTS, CSP, nosniff, frame deny, referrer and permissions policies on `/`, `/api/me` and `/api/documents`; NGINX version hidden |
| TC-AUD-01 | `06-audit.spec.ts` | N salary views by demo.hradmin produce exactly N `SALARY_READ` events in Loki, each with all 10 Section 11.1 fields |
| TC-E2E-05 | `06-audit.spec.ts` | demo.auditor sees the Audit dashboards with those events, and is refused the Platform dashboard |

**Run order:** the files run in order, one test at a time, because they share the demo accounts.
The audit file runs last so the events from earlier tests exist.

## What the tests need

**Demo accounts and emp_no values** come from Section 7.4 and `/database/README.md`.

**Passwords and one-time-password seeds** are never in the repository. Each is read from an
environment variable named after the user:

| Variable | For |
|---|---|
| `E2E_PASSWORD_DEMO_EMPLOYEE`, `E2E_PASSWORD_DEMO_EMPLOYEE2`, `E2E_PASSWORD_DEMO_MANAGER`, `E2E_PASSWORD_DEMO_HRADMIN`, `E2E_PASSWORD_DEMO_AUDITOR` | Directory passwords |
| `E2E_TOTP_DEMO_HRADMIN`, `E2E_TOTP_DEMO_AUDITOR` | Base32 one-time-password seeds for the MFA users |
| `E2E_DEV_USERNAME`, plus `E2E_PASSWORD_<that user>` | A `securehr-devs` account, for TC-AUTH-03 (skipped when not set) |

**Where Jenkins gets them:** from Vault `secret/securehr/ci`, key `e2e_env`, which holds these
lines in `NAME=value` form. `ci/scripts/fetch-ci-secrets.sh` writes it to a git-ignored file
that only Jenkins can read.

**Recording the OTP seeds (Jack):** when seeding demo.hradmin and demo.auditor, record the seed
Keycloak generates for each, and store it in `e2e_env`.

**Targets:**

| Variable | Default |
|---|---|
| `E2E_TARGET` | `staging` (portal `https://staging.securehr.local:8443`); set `production` for `https://securehr.local` |
| `PORTAL_URL`, `GRAFANA_URL`, `JENKINS_URL` | Override any URL directly |

## How to run

**In Jenkins:** stage 12 runs the suite on every `main` build, after the staging deploy.

**By hand:** needs Docker and the stack running. Before each run, apply the demo reset procedure
in the root README; it unlocks demo.employee2 after TC-E2E-06. From the repository root:

```bash
set -a; . ./e2e.env; set +a   # your own git-ignored file with the variables above
docker run --rm --network host --env-file <(env | grep -E '^(E2E_|PORTAL_URL|GRAFANA_URL|JENKINS_URL|SECUREHR_DOMAIN)') \
  -v "$PWD/e2e:/e2e" -w /e2e mcr.microsoft.com/playwright:v1.65.1-noble \
  sh -c 'npm ci && npx playwright test'
```

```bash
cd e2e && npx playwright show-report reports/html   # the report, with a screenshot per test
```

**Type-check only** (no stack needed):

```bash
cd e2e && npm ci && npx tsc --noEmit && npx playwright test --list
```

## TC-E2E-07: unlinked GitHub login (manual)

1. Use a GitHub account that has never been linked to a SecureHR directory account.
2. Open `https://securehr.local` and click **Sign in with GitHub**. Approve on GitHub.
3. Keycloak asks you to link the account to a directory account. Don't link it.
4. **Expected:** you have no roles, so either:
   - the portal shows "No access to the employee portal"; or
   - every API call returns 403.
5. Save screenshots of steps 2–4 to `/docs/evidence/e2e/TC-E2E-07-*.png`.

## Status

**Checked on Nov 28, 2026, without a running stack:**
- All 15 tests type-check in strict mode, and Playwright lists them all.
- The selectors match Arthi's portal (PR #26): labels, headings and buttons were read from its
  source, and the Keycloak fields use the standard login theme's ids.

**Not run yet:** no test has run against a real stack. They need Keycloak with the demo users,
the edge, the portal, both APIs and Grafana.

## Design decisions

| Decision | Why |
|---|---|
| Find elements by role and label, as a screen reader would | Tests break only when what a person sees changes, not the markup, and they double as an accessibility check |
| A fresh browser context per user | Each user has their own Keycloak session, as on separate machines |
| Count the page's own salary calls in TC-AUD-01 | The test proves exactly N calls give N audit events, even if the page fetches more than once |
| The auditor reads Loki through Grafana's query API | Proves what the auditor can see, with the auditor's own permissions |
| Certificate errors ignored in the browser | The test browser doesn't trust the mkcert authority; TLS is verified at the edge in Arthi's checks |

### Design patterns (Section 12.4)

| Pattern | Where |
|---|---|
| Page helpers (a light Page Object) | `support/login.ts`: Keycloak sign-in, OTP and portal entry in one place |
| Test data builder | `samplePdf()` builds the smallest valid PDF the Document API accepts |

## Versions

| Component | Version |
|---|---|
| Playwright | `@playwright/test` 1.65.1, image `mcr.microsoft.com/playwright:v1.65.1-noble` |
| TypeScript | 5.9.3 (same as /frontend) |
| One-time codes | `otpauth` 9.6.0 |
