#!/usr/bin/env bash
# Generates the single local TLS certificate used by the edge (Sections 0.3 and 0.11).
#
# Output (git-ignored, never committed):
#   edge/certs/securehr.pem      certificate for every public and staging hostname
#   edge/certs/securehr-key.pem  its private key
#   edge/certs/rootCA.pem        the mkcert CA certificate (public part only), mounted into
#                                containers that call https://auth.securehr.local
#
# Usage: edge/scripts/make-certs.sh   (run from anywhere; requires mkcert, see edge/CERTIFICATES.md)
set -euo pipefail

CERT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)/certs"

# Every hostname in Section 0.3, plus the staging. prefix for each (Section 11.3).
HOSTS=(
  securehr.local
  auth.securehr.local
  ci.securehr.local
  grafana.securehr.local
  directory.securehr.local
  staging.securehr.local
  staging.auth.securehr.local
  staging.ci.securehr.local
  staging.grafana.securehr.local
  staging.directory.securehr.local
)

if ! command -v mkcert >/dev/null 2>&1; then
  echo "mkcert is not installed. See edge/CERTIFICATES.md, step 1." >&2
  exit 1
fi

CA_ROOT="$(mkcert -CAROOT)"
if [[ ! -f "${CA_ROOT}/rootCA.pem" ]]; then
  echo "The mkcert local CA does not exist yet. Run 'mkcert -install' first (edge/CERTIFICATES.md, step 2)." >&2
  exit 1
fi

mkdir -p "${CERT_DIR}"
mkcert -cert-file "${CERT_DIR}/securehr.pem" -key-file "${CERT_DIR}/securehr-key.pem" "${HOSTS[@]}"

# Only the CA certificate is copied. rootCA-key.pem never leaves the mkcert CA folder.
cp "${CA_ROOT}/rootCA.pem" "${CERT_DIR}/rootCA.pem"

# The OWASP CRS NGINX image runs as a non-root user whose UID differs from the host user, and
# bind mounts keep host file modes, so the key must be world-readable inside this git-ignored
# folder. This is a local development key for *.securehr.local only; it protects nothing else.
chmod 644 "${CERT_DIR}/securehr.pem" "${CERT_DIR}/securehr-key.pem" "${CERT_DIR}/rootCA.pem"

echo "Certificates written to ${CERT_DIR}"
