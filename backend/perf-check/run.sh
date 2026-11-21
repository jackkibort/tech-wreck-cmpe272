#!/usr/bin/env sh
# Local G3 check: runs the Employee API image against the imported full dataset and drives
# directory searches with k6 (p95 < 300 ms). Not the team's CI load test (/perf, Samhitha).
#
# Needs: the securehr-prod mysql container running with the dataset imported, an image built with
# `docker build -t securehr/api:dev backend`, Python 3 with PyJWT and cryptography, and
# SECUREHR_APP_DB_PASSWORD in the environment (the securehr_app password; never committed).
# A throwaway issuer stands in for Keycloak; Vault is switched off for this run only.
set -eu
here=$(cd "$(dirname "$0")" && pwd)
work="$(cd "$here/.." && pwd)/target/perf-check"
# Git Bash on Windows: Docker and Python need D:/... paths.
if command -v cygpath >/dev/null 2>&1; then work=$(cygpath -m "$work"); here=$(cygpath -m "$here"); fi
: "${SECUREHR_APP_DB_PASSWORD:?set SECUREHR_APP_DB_PASSWORD}"
export MSYS_NO_PATHCONV=1

mkdir -p "$work/issuer/realms/securehr/.well-known" "$work/issuer/realms/securehr/protocol/openid-connect"
python -I "$here/mint.py" "$work"
cp "$here/search.js" "$work/"

cleanup() { docker rm -f perf-api perf-issuer >/dev/null 2>&1 || true; docker network rm perf-net >/dev/null 2>&1 || true; }
trap cleanup EXIT
cleanup
docker network create perf-net >/dev/null
docker run -d --name perf-issuer --network perf-net --network-alias issuer -v "$work/issuer:/srv:ro" -w /srv \
  python:3.12-alpine python -c "
import http.server as h
class J(h.SimpleHTTPRequestHandler):
    def guess_type(self, path): return 'application/json'
h.ThreadingHTTPServer(('', 8000), J).serve_forever()" >/dev/null
docker run -d --name perf-api --network securehr-prod_data --read-only --tmpfs /tmp --cap-drop ALL \
  -e KEYCLOAK_ISSUER_URI=http://issuer:8000/realms/securehr -e SPRING_CLOUD_VAULT_ENABLED=false \
  -e SECUREHR_DB_USERNAME=securehr_app -e SECUREHR_DB_PASSWORD="$SECUREHR_APP_DB_PASSWORD" \
  securehr/api:dev >/dev/null
docker network connect --alias api perf-net perf-api
until docker exec perf-issuer wget -qO- http://api:8080/api/health >/dev/null 2>&1; do sleep 2; done
docker run --rm --network perf-net -v "$work:/perf:ro" grafana/k6:1.0.0 run \
  --summary-trend-stats "avg,med,p(95),p(99),max" /perf/search.js
