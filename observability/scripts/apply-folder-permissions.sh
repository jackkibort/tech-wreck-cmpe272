#!/usr/bin/env bash
# Closes the Platform folder to Grafana Viewers, so AUDITOR (mapped to Viewer) sees only the
# Audit folder (Section 0.5, Grafana mapping). Grafana file provisioning creates folders but
# can't set their permissions, so this one call does it. Safe to run again.
#
# Needs: GRAFANA_TOKEN, a service-account token with the Admin role, created once by a
# SECUREHR_DEV user in Grafana (Administration → Service accounts). It's read from your shell
# only and never stored. GRAFANA_URL defaults to https://grafana.${SECUREHR_DOMAIN}.
set -euo pipefail

: "${GRAFANA_TOKEN:?Set GRAFANA_TOKEN to a Grafana service-account token (Admin role)}"
GRAFANA_URL="${GRAFANA_URL:-https://grafana.${SECUREHR_DOMAIN:-securehr.local}}"

api() {
  curl -fsS --max-time 10 -H "Authorization: Bearer ${GRAFANA_TOKEN}" \
    -H "Content-Type: application/json" "$@"
}

uid=$(api "${GRAFANA_URL}/api/folders" | python3 -c '
import json, sys
print(next(f["uid"] for f in json.load(sys.stdin) if f["title"] == "Platform"))')

# Replaces the folder's permission list: Editors may view; Viewers get nothing. Admins always
# have access, so developers (Grafana Admin) are unaffected.
api -X POST "${GRAFANA_URL}/api/folders/${uid}/permissions" \
  -d '{"items":[{"role":"Editor","permission":1}]}' >/dev/null

echo "Platform folder (${uid}) is now closed to Viewers."
