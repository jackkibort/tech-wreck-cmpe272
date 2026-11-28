#!/usr/bin/env bash
# Runs a k6 test in the pinned k6 container and saves results to perf/results/.
#
#   perf/run.sh search-edge     G3 search through the edge (firewall on)
#   perf/run.sh search-direct   G3 search straight to the API container (no edge, no firewall)
#   perf/run.sh burst           TC-SEC-03 burst against the edge (expects 429s)
#
# Needs the securehr-prod stack running, mkcert installed (k6 trusts its local authority), and
# either ACCESS_TOKEN or SECUREHR_PASSWORD (demo.employee's password, read from Vault) in your
# shell. Both are passed by name only, so they never appear in the process list or on disk.
set -euo pipefail

K6_IMAGE=grafana/k6:2.4.0
here="$(cd "$(dirname "$0")" && pwd)"
test="${1:?usage: perf/run.sh search-edge|search-direct|burst}"
domain="${SECUREHR_DOMAIN:-securehr.local}"

case "$test" in
  search-edge)   script=search.js; base="https://${domain}";  label=edge ;;
  search-direct) script=search.js; base="http://api:8080";    label=direct ;;
  burst)         script=burst.js;  base="https://${domain}";  label=edge ;;
  *) echo "unknown test: $test" >&2; exit 2 ;;
esac

ca_root="$(mkcert -CAROOT)"
mkdir -p "$here/results"

# The container joins the edge network (to reach api:8080 directly) and resolves the public
# hostnames to the host, where the edge publishes port 443.
docker run --rm \
  --user "$(id -u):$(id -g)" \
  --network securehr-prod_edge \
  --add-host "${domain}:host-gateway" \
  --add-host "auth.${domain}:host-gateway" \
  -v "$here:/perf" \
  -v "$ca_root/rootCA.pem:/certs/rootCA.pem:ro" \
  -e SSL_CERT_FILE=/certs/rootCA.pem \
  -e BASE_URL="$base" \
  -e TARGET_LABEL="$label" \
  -e KEYCLOAK_ISSUER_URI="${KEYCLOAK_ISSUER_URI:-https://auth.${domain}/realms/securehr}" \
  -e ACCESS_TOKEN \
  -e SECUREHR_USERNAME="${SECUREHR_USERNAME:-demo.employee}" \
  -e SECUREHR_PASSWORD \
  -e DURATION="${DURATION:-60s}" \
  "$K6_IMAGE" run --summary-trend-stats "avg,med,p(95),p(99),max" "/perf/scenarios/$script"
