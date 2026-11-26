#!/usr/bin/env bash
# Demo reset procedure (Section 13.3). Run from the repository root before a demo:
#   deploy/compose/reset-demo.sh
#
# 1. Clears every brute-force lockout in the securehr realm (demo.employee2 after S5).
# 2. Restarts the edge, which empties the in-memory rate-limit counters.
# Demo documents are restored separately with the docs-api seed step (/docs-api/README.md).
#
# Reads KEYCLOAK_ADMIN_USERNAME and KEYCLOAK_ADMIN_PASSWORD from the root .env (Section 0.9).
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/../.."
PROJECT="${COMPOSE_PROJECT_NAME:-securehr-prod}"
compose() {
  docker compose -p "${PROJECT}" --env-file .env -f deploy/compose/compose.yaml "$@"
}

if [[ ! -f .env ]]; then
  echo "No .env at the repository root; copy .env.example and fill it in first." >&2
  exit 1
fi
set -a
# shellcheck disable=SC1091
source .env
set +a
: "${KEYCLOAK_ADMIN_USERNAME:?KEYCLOAK_ADMIN_USERNAME must be set in .env}"
: "${KEYCLOAK_ADMIN_PASSWORD:?KEYCLOAK_ADMIN_PASSWORD must be set in .env}"

echo "1/2 Clearing Keycloak brute-force lockouts in realm securehr..."
# The password is passed through the environment of the exec, not on the command line, so it
# does not appear in the container's process list.
compose exec -T -e KCADM_PASSWORD="${KEYCLOAK_ADMIN_PASSWORD}" keycloak sh -c '
  /opt/keycloak/bin/kcadm.sh config credentials --config /tmp/kcadm.config \
    --server http://localhost:8080 --realm master \
    --user "'"${KEYCLOAK_ADMIN_USERNAME}"'" --password "$KCADM_PASSWORD" >/dev/null &&
  /opt/keycloak/bin/kcadm.sh delete attack-detection/brute-force/users \
    --config /tmp/kcadm.config -r securehr &&
  rm -f /tmp/kcadm.config
'

echo "2/2 Restarting the edge to reset rate limits..."
compose restart edge

echo "Done. Now restore the demo documents (see /docs-api/README.md)."
