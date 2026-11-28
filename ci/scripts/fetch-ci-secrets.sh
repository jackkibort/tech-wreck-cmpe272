#!/usr/bin/env bash
# Writes the CI credentials from Vault (secret/securehr/ci, Section 0.7) to ci/secrets/, where
# Compose mounts them into Jenkins as Docker secrets. Run before starting Jenkins, and again
# after the secrets are rotated.
#
# Needs: the vault CLI, VAULT_ADDR, and a Vault token allowed to read secret/securehr/ci
# (log in first with `vault login`). Keys read: github_username, github_token, e2e_env
# (the end-to-end test credentials, one NAME=value per line; see /e2e/README.md).
set -euo pipefail

: "${VAULT_ADDR:?Set VAULT_ADDR (Section 0.9)}"

secrets_dir="$(cd "$(dirname "$0")/.." && pwd)/secrets"
mkdir -p "$secrets_dir"
chmod 700 "$secrets_dir"

for key in github_username github_token e2e_env; do
  # umask keeps each file owner-only from the moment it is created.
  (umask 077 && vault kv get -mount=secret -field="$key" securehr/ci > "$secrets_dir/$key")
  echo "wrote ci/secrets/$key"
done
