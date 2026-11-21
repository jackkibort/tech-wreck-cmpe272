# Flyway history lives in its own schema (`securehr_flyway`)

- **Who:** Sabari
- **Date:** 2026-11-19
- **What:** The Employee API's Flyway history table is in a separate schema, `securehr_flyway`,
  not in `employees`.
- **Why:** Section 7.3 requires the original tables and data to stay unchanged. With the history
  table elsewhere, `employees` gains only indexes, and `securehr_migrator` needs no write access to
  employee data (only `SELECT, ALTER, INDEX`).
