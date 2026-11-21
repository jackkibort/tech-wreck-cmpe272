# Directory search query tuning for G3

- **Who:** Sabari
- **Date:** 2026-11-20
- **What:**
  - Page queries check "has a current department" per row instead of with `EXISTS`.
  - A Hibernate statement inspector adds `FORCE INDEX FOR ORDER BY (idx_employees_last_first)`
    to page queries.
  - Department lookups use a left join.
  - Search totals for the default view and the department filter are counted from department
    membership.
  - All totals are cached in a bounded in-memory LRU of 1,000 searches.
- **Why:** On the full dataset, MySQL's default plans took 0.5 to 4.7 seconds for the default
  view, the department filter and deep pages. After the changes, page queries take under 15 ms
  and a local k6 run gave p95 82 to 250 ms across name, department and default-view searches.
  Measurements are in `/backend/README.md`. JPA has no way to state a MySQL index hint;
  Hibernate's built-in hint support only emits `USE INDEX`, which would also block the
  first-name and hire-date indexes.
- **Trade-offs:**
  - The inspector matches the SQL shape Hibernate generates. If a Hibernate upgrade changes that
    shape, the hint silently stops applying; `NameOrderIndexHintTest` and the query timings in
    the README are the check.
  - Cached totals go stale only if the dataset changes, which the application never does; a
    restart clears them.
