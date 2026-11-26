#!/bin/sh
# Writes /config.json from the environment when the container starts, so the same image runs
# in securehr-prod and securehr-staging (Section 11.3). Only public values go here: the issuer
# URL and the public client ID (Section 0.9). Run by the nginx image's entrypoint.
set -eu

: "${KEYCLOAK_ISSUER_URI:?KEYCLOAK_ISSUER_URI must be set}"
: "${PORTAL_CLIENT_ID:?PORTAL_CLIENT_ID must be set}"

mkdir -p /tmp/securehr-config
printf '{"authority":"%s","clientId":"%s"}\n' "${KEYCLOAK_ISSUER_URI}" "${PORTAL_CLIENT_ID}" \
  > /tmp/securehr-config/config.json
