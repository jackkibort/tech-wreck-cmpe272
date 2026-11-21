# /database: Employees database (owner: Sabari)

The `mysql` service holds the company "backup" provided for the project: the
[datacharmer/test_db](https://github.com/datacharmer/test_db) MySQL Employees sample database.
It's about 300,000 synthetic employees, and none of them are real people. The data is
imported **unchanged**. The only additions are search indexes, which `/backend` applies
through Flyway (Section 7.3 of the plan).

## Contents

| Path | Purpose |
|---|---|
| `compose.yaml` | Area Compose file: the `mysql` service, `mysql-data` volume, `data` network |
| `compose.dev.yaml` | Developer-only override that publishes 3306 on `127.0.0.1` (never used in prod/staging) |
| `scripts/import.sh` | Downloads the pinned dataset, loads it and verifies counts and checksums |
| `scripts/bootstrap-users.sh` | Creates or rotates `securehr_app` and `securehr_migrator` |
| `scripts/lib.sh` | Shared helpers for the scripts |

## Pinned versions

| Component | Version |
|---|---|
| MySQL image | `mysql:8.4.13` (8.4 LTS) |
| test_db | commit `e324b56193ca506ab7cc1ab143a9153d8c4535d7` |

## How to run

You need Docker with the Compose plugin, Bash (Git Bash on Windows) and OpenSSL. Every
script reads `MYSQL_ROOT_PASSWORD` from the repository-root `.env`, which is git-ignored, and
uses Compose project `securehr-prod`. Override the project with `COMPOSE_PROJECT_NAME`.

```bash
# 1. Start MySQL, import the dataset and verify it (about 5 minutes; ~170 MB download).
database/scripts/import.sh            # add --force to drop and re-import

# 2. Create the database users. With VAULT_ADDR and VAULT_TOKEN set, the app credentials
#    go straight into Vault; otherwise the generated passwords are printed once.
database/scripts/bootstrap-users.sh

# 3. Apply the search indexes (see /backend/README.md, "api-migrate").
```

To inspect query plans from the host, add the dev override:
`docker compose -p securehr-prod -f database/compose.yaml -f database/compose.dev.yaml up -d`.

## Security settings

These are passed as server flags in `compose.yaml`. MySQL ignores world-writable `.cnf`
files, which is how bind mounts look on Windows, so a mounted config file isn't reliable.

| Setting | Why |
|---|---|
| `--require-secure-transport=ON` | All TCP connections must use TLS (Section 8.5) |
| `--tls-version=TLSv1.2,TLSv1.3` | No legacy protocols |
| `--local-infile=OFF` | Removes the `LOAD DATA LOCAL` attack surface |
| `--skip-name-resolve` | Grants use `%`; no reverse DNS lookups |
| No host ports | Only reachable on the internal `data` network (Section 4.2) |

TLS uses the certificates MySQL generates on first start, so the API connects with
`sslMode=REQUIRED`. The connection is encrypted, but the server certificate isn't checked
against a CA. Verifying it would mean sharing a private CA with the API container. That
trade-off is recorded in `/docs/decisions`.

## Database users (Section 0.8)

| User | Rights | Used by |
|---|---|---|
| `securehr_app` | `SELECT` on `employees.*`, `REQUIRE SSL` | The running `api` (credentials from Vault `secret/securehr/api/db`) |
| `securehr_migrator` | `SELECT, ALTER, INDEX` on `employees.*`; all rights on `securehr_flyway.*`; `REQUIRE SSL` | The one-shot `api-migrate` step only |

- **Why Flyway history is in `securehr_flyway`:** the original `employees` database gains no
  tables, and the migrator never needs write access to employee data.
- **Why users aren't created in Flyway:** creating users needs root, and passwords must
  never be stored in migration history.

## Import verification (Nov 19, 2026)

`import.sh` ran the dataset's own `test_employees_md5.sql` and `test_employees_sha.sql`.
Both reported `CRC OK` and `count OK`:

| Table | Expected rows | Found rows | SHA checksum |
|---|---:|---:|---|
| departments | 9 | 9 | ok |
| dept_emp | 331,603 | 331,603 | ok |
| dept_manager | 24 | 24 | ok |
| employees | 300,024 | 300,024 | ok |
| salaries | 2,844,047 | 2,844,047 | ok |
| titles | 443,308 | 443,308 | ok |

Also checked:
- an unencrypted login is refused;
- connections negotiate TLSv1.3, and TLSv1.1 is refused;
- `securehr_app` gets `INSERT command denied`.

## Demo identities (Section 7.4)

These records were chosen from the data. Jack seeds them into the directory as the `emp_no`
attribute.

| Username | emp_no | Name | Current department | Current title | Why this record |
|---|---:|---|---|---|---|
| demo.employee | 10012 | Patricio Bridgland | d005 Development | Senior Engineer | A regular (never-manager) member of demo.manager's department: the payslip and review owner in S1/S3. 10 salary rows for the chart |
| demo.manager | 110567 | Leon DasSarma | d005 Development | Manager | Current manager of d005 (`dept_manager.to_date = 9999-01-01`) |
| demo.employee2 | 10041 | Uri Lenart | d007 Sales | Senior Staff | A department other than demo.manager's, so it proves manager scope (403) |
| demo.hradmin | 10005 | Kyoichi Maliniak | d003 Human Resources | Senior Staff | An HR employee for the HR Admin role |
| demo.auditor | none | none | none | none | Auditors aren't linked to an employee |

Data facts these picks rely on:
- Every employee has at most one current department.
- Each department has exactly one current manager, who is also a current member of that
  department.
