# /backend: Employee API (owner: Sabari)

The Employee API is the only way into the employees database. It is read-only. Every request
must carry a Keycloak access token, and every answer is shaped by the caller's role: who they
are, which records they may see, and which fields of those records. Sensitive reads and every
refusal are written to the audit log.

The contract is the OpenAPI spec in [`/api-spec/employee-api.yaml`](../api-spec/employee-api.yaml).
The database it reads is described in [`/database/README.md`](../database/README.md).

## Endpoints

| Method and path | Who may call it | What it returns |
|---|---|---|
| `GET /api/me` | EMPLOYEE, MANAGER, HR_ADMIN | The caller (from the token only) and their own directory record |
| `GET /api/employees` | EMPLOYEE, MANAGER, HR_ADMIN | Directory search: name prefix, department, current title, hire-date range; paged, at most 100 rows |
| `GET /api/employees/{empNo}` | Self, MANAGER for current team members, HR_ADMIN | Employee detail. Birth date and current salary only for self and HR_ADMIN |
| `GET /api/employees/{empNo}/titles` | Same as detail | Title history |
| `GET /api/employees/{empNo}/salaries` | Self, HR_ADMIN | Salary history. Every read is audited as `SALARY_READ` |
| `GET /api/departments` | EMPLOYEE, MANAGER, HR_ADMIN | Departments with their current manager |
| `GET /api/org/{empNo}` | EMPLOYEE, MANAGER, HR_ADMIN | Chain from an employee to their department's current manager |
| `GET /api/export/employees` | HR_ADMIN | CSV of the search filters, at most 10,000 rows, audited as `EXPORT` |
| `GET /api/health` | Internal only (not routed by the edge) | `{"status":"UP"}` or `DOWN`, nothing else |

AUDITOR gets 403 on every endpoint, and anything not listed is denied.

**Rules that apply to every endpoint:**
- **401:** missing, expired, forged or wrong-audience token.
- **403:** a valid caller who isn't allowed. A 403 never reveals whether the record exists; 404
  is returned only after authorization passes.
- **Identity from the token only:** the caller's `emp_no` comes only from the validated token,
  never from the request.
- **Masking happens in the API:** restricted fields are left out of the response, not hidden
  by the UI.
- **One error format:** `{status, error, message, requestId}`. Messages never contain stack
  traces or internal details.

## How it is built

```
src/main/java/edu/sjsu/securehr/api/
├── controller/   HTTP only: maps requests to service calls. No business or authorization logic
├── service/      Use cases: authorize, load, mask, audit (EmployeeService, OrganizationService,
│                 ExportService), plus paging, CSV writing and search totals
├── security/     Token validation (issuer, audience, expiry, emp_no), role mapping, URL rules
│   └── policy/   One policy object per resource: EmployeeRecordPolicy, SalaryPolicy, ManagerScope
├── repository/   Spring Data repositories, search Specifications, the page query and its index hint
├── domain/       Read-only JPA entities for the original tables (marked @Immutable)
├── dto/          Response and request shapes. Entities are never returned directly
├── audit/        AuditTrail publishes events; AuditLogListener writes them as JSON lines
├── exception/    The one global exception handler and the error format
├── web/          Request ID filter (X-Request-ID, echoed in errors and audit events)
└── config/       JSON log format, clock
```

A request passes through these layers in order:
1. The **request ID filter** tags the request.
2. Spring Security validates the token and applies the **URL rules**: which roles may call
   which path, and deny everything else.
3. The **controller** passes the caller (`CurrentUser`) to a **service**.
4. The service asks the resource's **policy** whether this caller may see this record. A denial
   becomes a 403 and an `ACCESS_DENIED` audit event.
5. The service loads data through **repositories**, builds a **DTO** with the fields this caller
   may see, and records the audit event.

### Design patterns (Section 12.4)

| Pattern | Where |
|---|---|
| Layered architecture | `controller` → `service` → `repository` |
| Repository | `repository/*Repository` (Spring Data, read-only) |
| Data Transfer Object | `dto/*`; entities never leave the service layer |
| Specification | `EmployeeSpecifications` builds the search from optional filters |
| Strategy / Policy | `security/policy`: one policy object per resource decides access |
| Observer / event listener | `AuditTrail` publishes `AuditEvent`s; `AuditLogListener` is the only writer |

## Configuration

Every value comes from the Section 0.9 environment variables. No secret is in any file, image
or Compose file.

| Variable | Purpose |
|---|---|
| `KEYCLOAK_ISSUER_URI` | Token issuer. Keys are fetched from it on the first request, so the API can start before Keycloak |
| `API_AUDIENCE` | Required `aud` value (default `securehr-api`) |
| `VAULT_ADDR`, `API_VAULT_ROLE_ID`, `API_VAULT_SECRET_ID` | Vault AppRole login. The database username and password are read from `secret/securehr/api/db` |
| `MYSQL_HOST`, `MYSQL_PORT`, `MYSQL_DATABASE` | Database location (defaults `mysql`, `3306`, `employees`). TLS is always required |

The API **refuses to start** if Vault rejects its login, if its policy can't read
`secret/securehr/api/db`, or if the secret is missing. It never falls back to another source.

## How to run

You need Docker with the Compose plugin. Run everything from the repository root.

```bash
# 1. Database: import and create users first (see /database/README.md).

# 2. "api-migrate": apply the search indexes with the migrator user (one-shot, idempotent).
#    The password is typed into your shell for this command only; it is never stored in .env.
FLYWAY_PASSWORD='<securehr_migrator password>' \
  docker compose -p securehr-prod --env-file .env -f backend/compose.dev.yaml \
  --profile migrate run --rm api-migrate

# 3. Start the database and the API together (without the rest of the stack).
docker compose -p securehr-prod --env-file .env -f backend/compose.dev.yaml up -d --build
```

In the full stack, the root Compose file includes `backend/compose.yaml`. Only the edge
reaches the API, at `https://securehr.local/api/`.

**Container hardening:**
- Runs as a non-root user (uid 10001).
- Read-only root filesystem.
- All Linux capabilities dropped, and `no-new-privileges` set.

## How to test

```bash
cd backend
./mvnw verify        # format check, Checkstyle, unit tests, integration tests, coverage gate
./mvnw test          # unit tests only (no Docker needed)
./mvnw spotless:apply  # fix formatting
```

The integration tests (`*IT`) start the same MySQL version in Testcontainers. Flyway builds the
original schema plus a small fixed dataset (`src/test/resources/db/testdata`), then applies the
real index migration. Tokens are signed with a test key. `VaultCredentialsIT` starts a real
Vault container set up as in Section 0.7.

**Current result:**
- 88 tests (26 unit, 62 integration), all passing.
- Coverage is 95% overall: `service` 98%, `security` 95%, `security.policy` 100% (gate: 70%).
- Reports are in [`/docs/evidence/backend`](../docs/evidence/backend). Regenerate them with
  `scripts/collect-evidence.sh`.

| Test case | Test |
|---|---|
| TC-API-01 no token → 401 | `TokenValidationIT.tcApi01_*` |
| TC-API-02 expired, forged, wrong audience or issuer → 401 | `TokenValidationIT.tcApi02_*` |
| TC-API-03 own salary → 200 | `EmployeeAccessMatrixIT.tcApi03_*` |
| TC-API-04 another's salary → 403, audited | `EmployeeAccessMatrixIT.tcApi04_*` |
| TC-API-05 manager reads team member, no salary fields | `EmployeeAccessMatrixIT.tcApi05_*` |
| TC-API-06 manager reads out-of-department employee → 403 | `EmployeeAccessMatrixIT.tcApi06_*` (other department, transferred out, former employee, unknown) |
| TC-API-07 HR export: CSV, ≤ 10,000 rows, audited | `ExportIT.tcApi07_*`, `ExportServiceTest.tcApi07_*` |
| TC-API-08 auditor → 403 on every endpoint | `TokenValidationIT.tcApi08_*` |
| TC-API-09 page size above 100 → 100 | `EmployeeSearchIT.tcApi09_*` |
| TC-API-10 search by name, department, title, hire dates | `EmployeeSearchIT.tcApi10_*` |

Other tests cover:
- the department list and the org chain;
- CSV escaping and spreadsheet-formula neutralization;
- the search-total cache;
- the index hint;
- the JSON log format;
- token claim parsing.

## Performance (G3: search p95 under 300 ms on 300K records)

Measured on the full imported dataset (300,024 employees), on the developer laptop.

**Query plans** (`EXPLAIN ANALYZE`, warm):

| Problem found | Fix | Before | After |
|---|---|---|---|
| The default view and the department filter sorted every current employee before taking 25 | Page queries check "has a current department" per row (`(select count(*) ...) > 0`), so MySQL walks the name index in order and stops after one page | 2,691 ms / 470 ms | under 1 ms |
| From page offset ~1,000, MySQL switched back to a full sort | `NameOrderIndexHint` adds `FORCE INDEX FOR ORDER BY (idx_employees_last_first)` to page queries only | 2,043 ms | 13 ms |
| The department lookup for each page scanned each department's members | `left join fetch` makes MySQL start from the 25 employees on the page | 201 ms | 0.3 ms |
| Totals recounted on every page | Default and department totals come from the membership index; all totals cached (bounded, 1,000 searches) because the data never changes | 2,000 ms | 0 ms after the first request |

Selective filters keep their fast plans. A rare title ("Manager") takes 0.2 ms with or without
these changes.

**Load test:** `perf-check/run.sh` runs k6 against the API on the full dataset, with 12
concurrent users for 60 seconds, about 110 requests per second:

| Scenario | p95 |
|---|---|
| Name search | 250 ms |
| Department filter | 201 ms |
| Default directory view, random pages | 82 ms |

**Known limits:**
- Very broad filters (a one-letter name, "hired after 1985") take about a second to count the
  first time, because they match more than 100,000 people. Repeats and later pages are instant.
- A later run on the same laptop, while its CPU was throttled to 60% clock speed, was about 2×
  slower across all queries, including SQL unchanged by this work.
- The official G3 figure is Samhitha's k6 run in `/perf`, through the edge with the firewall on.

## Security scans

Results as of Nov 20, 2026:

| Scan | Result |
|---|---|
| Semgrep 1.128.1 (`p/java`, `p/secrets`, `p/owasp-top-ten`) | 0 findings |
| Trivy 0.65.0, source and Dockerfile | 0 HIGH/CRITICAL, 0 secrets, 0 misconfigurations |
| Trivy 0.65.0, `securehr/api` image | 0 HIGH/CRITICAL with [`.trivyignore.yaml`](.trivyignore.yaml) |

`.trivyignore.yaml` accepts two Spring Web MVC advisories. Both need features this API doesn't
have: XSLT views and Server-Sent Events with view fragments. No Spring Boot 3.x release fixes
them yet, and we stay on the latest 3.x as Section 0.1 requires; no move to Spring Boot 4 is
needed. Each entry states why and expires on Jan 31, 2027. CI runs Trivy with
`--ignorefile backend/.trivyignore.yaml`.

## Decisions

Recorded in `/docs/decisions`. The short version:

| Decision | Why |
|---|---|
| Flyway history in its own schema `securehr_flyway` | The original database gains no tables; the migrator never needs write access to employee data |
| Migrations run by the official `flyway/flyway` image (`api-migrate`), never by the API | The API's database user is SELECT-only, and the API never holds schema-change rights |
| TLS to MySQL is encrypted but the certificate isn't CA-verified (`sslMode=REQUIRED`) | Verifying would need a private CA shared with the API container; the database is only reachable on the internal `data` network |
| Token keys fetched on first request, not at startup | Compose start order doesn't block the API on Keycloak. Requests fail with 401 until Keycloak answers |
| A 403 is returned before any existence check | Unauthorized callers can't probe which employee numbers exist |
| The org chart and department list are open to every portal role, and 404 for unknown employees | They hold directory fields only, which every role may already search |
| Search totals cached in memory | The dataset is never changed by the application (Section 7.3); a restart picks up a re-import |
| Spring Boot 3.5.16, with patched Tomcat, Jackson and Spring Cloud | Section 0.1 pins Spring Boot 3.x; the patch overrides clear the Trivy findings that have a 3.x fix |

## Pinned versions

| Component | Version |
|---|---|
| Java | 21 (Eclipse Temurin `21.0.13_9`, JDK for the build, JRE for the runtime) |
| Spring Boot | 3.5.16 (Tomcat 10.1.62, Jackson 2.21.8 overrides) |
| Spring Cloud | 2025.0.3 (Spring Cloud Vault 4.3.3) |
| MySQL (tests) | `mysql:8.4.13`, same as `/database` |
| Flyway (`api-migrate`) | `flyway/flyway:11.7.2-alpine` |
| Vault (tests) | `hashicorp/vault:1.20.4` |
| Checks | Spotless (google-java-format), Checkstyle 10.26.1, JaCoCo 0.8.16 |
| perf-check | `grafana/k6:1.0.0`, `python:3.12-alpine` |
