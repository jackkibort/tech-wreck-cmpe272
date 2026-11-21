# Department list and org chart are open to every portal role

- **Who:** Sabari
- **Date:** 2026-11-20
- **What:** `GET /api/departments` and `GET /api/org/{empNo}` are allowed for EMPLOYEE, MANAGER
  and HR_ADMIN (Section 8.2, "Org chart"), return directory fields only, and answer 404 for an
  unknown employee number.
- **Why:** They expose only what every portal role can already find through directory search
  (names, departments, current titles). Other per-record endpoints check authorization before
  existence and never return 404 to an unauthorized caller.
