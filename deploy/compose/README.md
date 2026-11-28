# /deploy/compose: root Compose file (owner: Arthi)

The root Compose file starts the whole SecureHR stack as one Compose project. It owns only the
shared networks and the list of area files; every service is defined in its owner's folder
(Section 14, "Compose split").

## Contents

| Path | Purpose |
|---|---|
| `compose.yaml` | Declares the shared networks and includes each area's `compose.yaml` |
| `../../.env.example` | Template for the git-ignored root `.env` (Section 0.9) |

## Included area files

| Area file | Services | Owner |
|---|---|---|
| `database/compose.yaml` | mysql | Sabari |
| `backend/compose.yaml` | api, api-migrate (profile `migrate`) | Sabari |

Other areas are added to the `include:` list as their Compose file lands on main.

## Networks (Section 4.2)

| Network | Members |
|---|---|
| edge | edge, ui, api, docs-api, keycloak, grafana, jenkins, ldap (admin UI), smee |
| app | api, docs-api, keycloak, vault, jenkins |
| data | api, mysql, docs-api, docs-db, keycloak, keycloak-db |
| directory | keycloak, ldap |
| logs | alloy, loki, grafana |

Area files repeat the network names they use; Compose merges them into one network per name.

## How to run

From the repository root:

```bash
cp .env.example .env      # then fill in the blank values
docker compose -p securehr-prod --env-file .env -f deploy/compose/compose.yaml up -d
```

`--env-file .env` matters: without it Compose looks for `.env` next to this file instead of at the
repository root.

Check the merged configuration without starting anything:

```bash
docker compose -p securehr-prod --env-file .env -f deploy/compose/compose.yaml config --quiet
```

## Design decisions

- **Include, not one big file.** Owners change their own area file without merge conflicts in a
  shared file (Section 20, "Merge conflicts").
- **Project name on the command line.** The same files serve `securehr-prod` and
  `securehr-staging` (Section 0.11), so the name is passed with `-p` rather than fixed in the file.
