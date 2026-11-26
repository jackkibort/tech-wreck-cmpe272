# SecureHR: Enterprise HR Portal (CMPE-272 term project)

SecureHR is a proof-of-concept HR portal over the company's employee database
([datacharmer/test_db](https://github.com/datacharmer/test_db), about 300,000 synthetic
employees). It shows how an enterprise application is secured at every layer: one sign-in for
everything, access based on role and on whose record is requested, a protected edge, managed
secrets, a full audit trail, and a CI/CD pipeline that blocks insecure code.

## Features

### Required

| Requirement | How SecureHR meets it |
|---|---|
| Use the provided database | test_db imported **unchanged** into MySQL 8.4, checksums verified (`/database`) |
| SSO / AD authentication | Keycloak SSO federating an LDAP directory (LLDAP, standing in for Active Directory) |
| SSL/TLS | HTTPS only with HSTS at the edge; TLS from the APIs to MySQL and PostgreSQL |
| Web portal with SSO roles | React portal: directory search, profiles, title and salary history, org chart; 4 business roles enforced in the API |
| Jenkins integrated with SSO and GitHub | Jenkins login through Keycloak; multibranch pipeline from GitHub with status on every pull request |
| GitHub in SSO (optional) | "Sign in with GitHub" through Keycloak, linked to directory accounts |

### Highlighted extras

- **Layered security:** web application firewall (ModSecurity + OWASP CRS, blocking mode), per-IP
  rate limits, MFA for HR Admins and Auditors, HashiCorp Vault for secrets, read-only database
  user, field masking in the API, network segmentation, hardened containers.
- **Document repository:** role-restricted policies, payslips and performance reviews.
- **Audit and monitoring:** every salary read, export, document action, firewall block and
  rate-limit hit is an audit event, shown on Grafana dashboards with alerts.
- **DevSecOps pipeline:** gitleaks, Semgrep, Trivy (dependencies and images), OWASP ZAP,
  Playwright end-to-end tests and lint gates.
- **HR Admin CSV export:** capped at 10,000 rows and audited.

## Architecture

```text
                  ┌────────────────────────────── Docker host ──────────────────────────────┐
 Browser ─HTTPS─▶ │  edge (NGINX + ModSecurity/OWASP CRS, TLS, rate limit, headers)          │
                  │    │            │                 │               │          │          │
                  │    ▼            ▼                 ▼               ▼          ▼          │
                  │   ui          api            docs-api          keycloak   grafana      │
                  │ (React)  (Employee API)  (Document API)       (SSO/MFA)  (dashboards)  │
                  │               │   │            │   │            │    │        ▲        │
                  │          TLS  │   └─ vault ─┘ │ TLS          │    └─▶ ldap   │        │
                  │               ▼  (secrets)     ▼             ▼   (directory)  │        │
                  │            mysql            docs-db +    keycloak-db           │        │
                  │        (employees DB)    docs file volume                     │        │
                  │  all services ─ stdout ─▶ alloy ─▶ loki ──────────────────────┘        │
                  │  jenkins ◀── smee relay ◀── GitHub (webhooks)                           │
                  └─────────────────────────────────────────────────────────────────────────┘
```

Only the edge publishes host ports (443, 80). Databases sit on an internal network that the edge
cannot reach. Public hostnames:

| Hostname | Service |
|---|---|
| `https://securehr.local` | Portal (`/`), Employee API (`/api/`), Document API (`/api/documents`) |
| `https://auth.securehr.local` | Keycloak |
| `https://ci.securehr.local` | Jenkins |
| `https://grafana.securehr.local` | Grafana |
| `https://directory.securehr.local` | LLDAP admin UI |

Staging uses the same names with a `staging.` prefix on port 8443.

## Setup from a clean machine

**Prerequisites:** Docker Engine or Docker Desktop with the Compose plugin, 16 GB RAM
recommended, about 25 GB free disk, Git, Bash (Git Bash on Windows), OpenSSL, mkcert,
Node.js 22 (only for frontend development).

1. **Clone** the repository and create the environment file:

   ```bash
   git clone https://github.com/jackkibort/tech-wreck-cmpe272.git securehr
   cd securehr
   cp .env.example .env    # fill in the blank values; .env is git-ignored
   ```

2. **Certificates and hosts file:** follow [edge/CERTIFICATES.md](edge/CERTIFICATES.md)
   (`mkcert -install`, `edge/scripts/make-certs.sh`, one hosts-file line).
3. **Directory, identity and secrets:** start LLDAP, Keycloak and Vault, unseal Vault and seed
   the demo users and secrets as described in `/directory`, `/identity` and `/secrets` (Jack).
4. **Database:** import and verify the dataset, create the database users and apply the
   indexes: [database/README.md](database/README.md) (Sabari).
5. **Start the stack:**

   ```bash
   docker compose -p securehr-prod --env-file .env -f deploy/compose/compose.yaml up -d --build
   ```

6. **Open** <https://securehr.local> and sign in with a demo account.

See [deploy/compose/README.md](deploy/compose/README.md) for the Compose layout.

## Demo accounts

Passwords are generated at seed time and stored in Vault; they are never committed.

| Username | Role(s) | Used for |
|---|---|---|
| demo.employee | Employee | Own profile, salary chart, payslip download (S1, S2) |
| demo.manager | Employee, Manager | Team member profiles without salaries (S3) |
| demo.employee2 | Employee | Out-of-department 403; brute-force lockout demo (S3, S5) |
| demo.hradmin | Employee, HR Admin (MFA) | Any record, uploads, CSV export (S4) |
| demo.auditor | Auditor (MFA) | Grafana audit dashboards only (S7) |

Linked employee numbers are listed in [database/README.md](database/README.md).

**Reset before a demo:** `deploy/compose/reset-demo.sh`, then restore the demo documents
(see [deploy/compose/README.md](deploy/compose/README.md#demo-reset-section-133)).

## Running the tests

| Suite | Command | Owner |
|---|---|---|
| Employee API (unit, access-control matrix, integration) | `cd backend && ./mvnw verify` | Sabari |
| Frontend (Vitest) | `cd frontend && npm ci && npm test` | Arthi |
| Frontend lint and format | `cd frontend && npm run lint && npm run format:check` | Arthi |
| Firewall payloads (TC-SEC-01, stack running) | `edge/scripts/waf-test.sh` | Arthi |
| Document API | see `/docs-api` | Jack |
| End-to-end (Playwright), load (k6) | see `/e2e`, `/perf` | Samhitha |

Every suite also runs in Jenkins on each pull request (see `/ci`).

**Test evidence** (screenshots and reports of passing runs) is in [docs/evidence](docs/evidence),
one folder per area. Decisions are logged in [docs/decisions](docs/decisions).

## Repository map

| Folder | Contents | Owner |
|---|---|---|
| `/directory` | LLDAP setup, groups, demo user seed | Jack |
| `/identity` | Keycloak realm export, federation, GitHub broker, MFA | Jack |
| `/secrets` | Vault configuration, policies, AppRoles | Jack |
| `/docs-api` | Document API | Jack |
| `/backend` | Employee API | Sabari |
| `/database` | Dataset import, checksum verification, DB users | Sabari |
| `/api-spec` | OpenAPI specs for both APIs | Sabari, Jack |
| `/frontend` | React portal | Arthi |
| `/edge` | NGINX + ModSecurity edge, certificates guide, firewall payloads | Arthi |
| `/deploy/compose` | Root Compose file, `.env.example`, demo reset | Arthi |
| `/ci` | Jenkins configuration as code, Jenkinsfile, scanners | Samhitha |
| `/observability` | Alloy, Loki, Grafana dashboards and alerts | Samhitha |
| `/e2e`, `/perf` | Playwright and k6 tests | Samhitha |
| `/deploy/aws` | AWS deployment guide | Samhitha |
| `/docs` | Evidence and decision log | All |

## Team

| Member | Area |
|---|---|
| Jack Kibort | Identity, directory, secrets, Document API |
| Sabari Iyyappan Duraipandian | Employee API and database |
| Arthi Chandra Sekar | Frontend, edge, integration and root README |
| Samhitha Banda | CI/CD, observability, end-to-end and performance testing |

The project report (with design patterns, diagrams, use and test cases) is submitted on Canvas.
