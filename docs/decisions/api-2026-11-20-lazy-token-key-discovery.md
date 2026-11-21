# The API fetches Keycloak's signing keys on the first request

- **Who:** Sabari
- **Date:** 2026-11-20
- **What:** Token signing keys are discovered from `KEYCLOAK_ISSUER_URI` when the first request
  arrives, not at startup.
- **Why:** With startup discovery, the API crashes whenever it starts before Keycloak, and the
  stack's start order would have to be managed. Deferring it lets the API start and report
  healthy. Until Keycloak answers, every request is refused with 401, so there is no window where
  tokens go unchecked.
