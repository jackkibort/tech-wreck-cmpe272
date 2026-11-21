#!/usr/bin/env bash
# Imports the professor-provided datacharmer/test_db dataset into the mysql service
# unchanged, then verifies row counts and checksums with the dataset's own scripts
# (Section 7.3). Usage: database/scripts/import.sh [--force]
#   --force  drop and re-import an existing employees database (destroys added indexes
#            and the Flyway history; re-run the api-migrate step afterwards).

source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

# Pinned dataset revision so every teammate imports byte-identical data.
TEST_DB_REPO="https://github.com/datacharmer/test_db.git"
TEST_DB_COMMIT="e324b56193ca506ab7cc1ab143a9153d8c4535d7"
CACHE_DIR="${DATABASE_DIR}/.cache/test_db"
CONTAINER_DIR="/tmp/test_db"

force=false
[[ "${1:-}" == "--force" ]] && force=true

fetch_dataset() {
  if [[ ! -d "${CACHE_DIR}/.git" ]]; then
    log "Cloning test_db into ${CACHE_DIR}"
    git clone --quiet "${TEST_DB_REPO}" "${CACHE_DIR}"
  fi
  git -C "${CACHE_DIR}" fetch --quiet origin
  git -C "${CACHE_DIR}" checkout --quiet "${TEST_DB_COMMIT}"
  log "test_db pinned at ${TEST_DB_COMMIT}"
}

guard_existing_database() {
  local exists
  exists="$(mysql_root -N -e "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='employees'")"
  if [[ "${exists}" != "0" ]] && ! ${force}; then
    die "Database 'employees' already exists. Re-run with --force to drop and re-import it."
  fi
}

load_dataset() {
  log "Copying dataset into the container"
  compose exec -T mysql rm -rf "${CONTAINER_DIR}"
  compose cp "${CACHE_DIR}" "mysql:${CONTAINER_DIR}"
  log "Loading employees.sql (several minutes)"
  # employees.sql sources its .dump files by relative path, so run it from the dataset folder.
  compose exec -T -w "${CONTAINER_DIR}" mysql \
    sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot < employees.sql' >/dev/null
}

# Runs one of the dataset's verification scripts and fails unless both CRC and count are OK.
verify_with() {
  local script="$1" output
  log "Verifying with ${script}"
  output="$(compose exec -T -w "${CONTAINER_DIR}" mysql \
    sh -c "MYSQL_PWD=\"\$MYSQL_ROOT_PASSWORD\" mysql -uroot -t employees < ${script}")"
  printf '%s\n' "${output}"
  grep -Eq '\|[[:space:]]*CRC[[:space:]]*\|[[:space:]]*OK' <<<"${output}" || die "${script}: checksum mismatch"
  grep -Eq '\|[[:space:]]*count[[:space:]]*\|[[:space:]]*OK' <<<"${output}" || die "${script}: row count mismatch"
}

cleanup_container_copy() {
  compose exec -T mysql rm -rf "${CONTAINER_DIR}"
}

main() {
  [[ -f "${ENV_FILE}" ]] || die "Missing ${ENV_FILE}; copy .env.example and set MYSQL_ROOT_PASSWORD."
  fetch_dataset
  compose up -d mysql
  wait_for_mysql
  guard_existing_database
  load_dataset
  verify_with test_employees_md5.sql
  verify_with test_employees_sha.sql
  cleanup_container_copy
  log "Import complete: row counts and checksums match the published values."
}

main "$@"
