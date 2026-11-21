#!/usr/bin/env bash
# Creates (or rotates) the two least-privilege database users from Section 0.8:
#   securehr_app       SELECT only on employees, TLS required. Used by the running api.
#   securehr_migrator  schema changes only (indexes via Flyway, history in securehr_flyway).
#                      Never used by the running api.
#
# Users are created here rather than in a Flyway migration because user creation needs root
# and passwords must never be stored in migration history (see /docs/decisions).
#
# Passwords are taken from SECUREHR_APP_DB_PASSWORD / SECUREHR_MIGRATOR_DB_PASSWORD in the
# caller's shell if set, otherwise generated. When VAULT_ADDR and VAULT_TOKEN are set, the
# app credentials are written to secret/securehr/api/db (Section 0.7); otherwise they are
# printed once to the terminal and must be stored by the operator. Nothing is written to disk.

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

APP_USER="securehr_app"
MIGRATOR_USER="securehr_migrator"
FLYWAY_SCHEMA="securehr_flyway"
APP_SECRET_PATH="securehr/api/db"

generate_password() {
  openssl rand -hex 24
}

# Passwords are embedded in SQL sent over stdin, so restrict them to a quote-free alphabet.
validate_password() {
  [[ "$1" =~ ^[A-Za-z0-9._~+=-]{16,}$ ]] || die "Passwords must be 16+ chars of [A-Za-z0-9._~+=-]"
}

user_sql() {
  local app_password="$1" migrator_password="$2"
  cat <<SQL
CREATE USER IF NOT EXISTS '${APP_USER}'@'%' IDENTIFIED BY '${app_password}' REQUIRE SSL;
ALTER USER '${APP_USER}'@'%' IDENTIFIED BY '${app_password}' REQUIRE SSL;
-- Revoke first so re-runs converge to exactly these grants (GRANT is additive).
REVOKE IF EXISTS ALL PRIVILEGES ON employees.* FROM '${APP_USER}'@'%';
GRANT SELECT ON employees.* TO '${APP_USER}'@'%';

CREATE USER IF NOT EXISTS '${MIGRATOR_USER}'@'%' IDENTIFIED BY '${migrator_password}' REQUIRE SSL;
ALTER USER '${MIGRATOR_USER}'@'%' IDENTIFIED BY '${migrator_password}' REQUIRE SSL;
-- Index changes only on the dataset; it can never write employee data.
REVOKE IF EXISTS ALL PRIVILEGES ON employees.* FROM '${MIGRATOR_USER}'@'%';
GRANT SELECT, ALTER, INDEX ON employees.* TO '${MIGRATOR_USER}'@'%';
-- Flyway's history table lives in its own schema so the original database gains no tables.
CREATE DATABASE IF NOT EXISTS ${FLYWAY_SCHEMA};
GRANT ALL PRIVILEGES ON ${FLYWAY_SCHEMA}.* TO '${MIGRATOR_USER}'@'%';
SQL
}

store_in_vault() {
  local password="$1"
  curl --fail --silent --show-error \
    --header "X-Vault-Token: ${VAULT_TOKEN}" \
    --request POST \
    --data "{\"data\":{\"username\":\"${APP_USER}\",\"password\":\"${password}\"}}" \
    "${VAULT_ADDR%/}/v1/secret/data/${APP_SECRET_PATH}" >/dev/null
}

main() {
  local app_password="${SECUREHR_APP_DB_PASSWORD:-$(generate_password)}"
  local migrator_password="${SECUREHR_MIGRATOR_DB_PASSWORD:-$(generate_password)}"
  validate_password "${app_password}"
  validate_password "${migrator_password}"

  compose up -d mysql
  wait_for_mysql
  user_sql "${app_password}" "${migrator_password}" | mysql_root
  log "Users ${APP_USER} and ${MIGRATOR_USER} created or rotated."

  if [[ -n "${VAULT_ADDR:-}" && -n "${VAULT_TOKEN:-}" ]]; then
    store_in_vault "${app_password}"
    log "App credentials stored in Vault at secret/${APP_SECRET_PATH}."
  elif [[ -z "${SECUREHR_APP_DB_PASSWORD:-}" ]]; then
    log "Vault not configured. Store this generated app password now; it is not shown again:"
    printf '%s password: %s\n' "${APP_USER}" "${app_password}" >&2
  fi
  if [[ -z "${SECUREHR_MIGRATOR_DB_PASSWORD:-}" ]]; then
    log "Store this generated migrator password now; it is not shown again:"
    printf '%s password: %s\n' "${MIGRATOR_USER}" "${migrator_password}" >&2
  fi
}

main "$@"
