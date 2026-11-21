#!/usr/bin/env bash
# Shared helpers for the database scripts. Sourced, never executed directly.

set -euo pipefail

# Native host paths (D:/... under Git Bash on Windows) so git and docker binaries resolve them
# even with MSYS path conversion disabled below.
native_pwd() { pwd -W 2>/dev/null || pwd; }
DATABASE_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && native_pwd)"
REPO_ROOT="$(cd "${DATABASE_DIR}/.." && native_pwd)"
ENV_FILE="${ENV_FILE:-${REPO_ROOT}/.env}"
# Same project name as the demo stack (Section 0.11) so these scripts act on its mysql.
COMPOSE_PROJECT="${COMPOSE_PROJECT_NAME:-securehr-prod}"

# Prevent Git Bash on Windows from rewriting container paths such as /tmp into host paths.
export MSYS_NO_PATHCONV=1

compose() {
  docker compose --env-file "${ENV_FILE}" -p "${COMPOSE_PROJECT}" \
    -f "${DATABASE_DIR}/compose.yaml" "$@"
}

# Runs SQL from stdin as root over the container's local socket. The root password is read
# from the container's own environment, so it never appears on a host command line.
mysql_root() {
  compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -uroot "$@"' -- "$@"
}

log() {
  printf '[%s] %s\n' "$(date -u +%H:%M:%S)" "$*" >&2
}

die() {
  log "ERROR: $*"
  exit 1
}

wait_for_mysql() {
  log "Waiting for mysql to become healthy..."
  for _ in $(seq 1 60); do
    if [[ "$(docker inspect -f '{{.State.Health.Status}}' "$(compose ps -q mysql)" 2>/dev/null)" == "healthy" ]]; then
      return 0
    fi
    sleep 5
  done
  die "mysql did not become healthy in time"
}
