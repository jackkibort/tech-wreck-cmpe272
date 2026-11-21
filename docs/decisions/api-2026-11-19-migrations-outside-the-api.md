# Index migrations run as a one-shot step, never inside the API

- **Who:** Sabari
- **Date:** 2026-11-19
- **What:** The search indexes are applied by the `api-migrate` Compose service (official
  `flyway/flyway` image) as `securehr_migrator`. The running API has Flyway disabled.
- **Why:** The API's database user is SELECT-only (Section 0.8). Running migrations at startup
  would give the long-running, internet-facing service schema-change rights. The migrator's
  password is typed in by the operator for the one command and never stored in `.env`.
