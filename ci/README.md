# /ci: Jenkins CI/CD pipeline (owner: Samhitha)

Jenkins builds, tests and scans every branch and pull request of the GitHub repository, and
reports the result back to the pull request. `main` cannot merge without a green build
(Section 11 of the implementation plan). Jenkins is set up entirely from files in this folder, so
it can be rebuilt from scratch. There is no manual setup in the Jenkins UI.

## What's here

| Path | Purpose |
|---|---|
| `Jenkinsfile` | The pipeline: stages 1–10 of Section 11.2 |
| `compose.yaml` | Area Compose file: the `jenkins` and `smee` services (Section 0.2) |
| `compose.local.yaml` | Local development only: publishes Jenkins on `http://localhost:8090` before the edge exists |
| `jenkins/Dockerfile` | Jenkins LTS plus the Docker CLI, pinned plugins and configuration as code |
| `jenkins/plugins.txt` | Pinned Jenkins plugins |
| `jenkins/casc/security.yaml` | Keycloak sign-in; only `SECUREHR_DEV` gets in |
| `jenkins/casc/credentials.yaml` | GitHub credential, read from Docker secrets that come from Vault |
| `jenkins/casc/jobs.yaml` | The multibranch pipeline job over the GitHub repository |
| `jenkins/docker-socket-entrypoint.sh` | Gives the `jenkins` user access to the host Docker socket, then starts Jenkins as `jenkins` |
| `smee/Dockerfile` | Webhook relay from smee.io to Jenkins |
| `scanners/gitleaks.toml` | Secrets scan rules (gitleaks defaults plus a documented allowlist) |
| `scripts/fetch-ci-secrets.sh` | Copies the CI credentials from Vault into git-ignored files |
| `../.github/CODEOWNERS` | Folder owners from Section 14 (GitHub reads this file only from `.github/`, the repository root or `docs/`) |
| `../.github/pull_request_template.md` | Pull request checklist |

## Pipeline (Section 11.2)

Every tool runs in its own container with a pinned image, started on the host Docker engine.
To change a tool version, edit the `environment` block at the top of the `Jenkinsfile`.

| # | Stage | Tool and image | Fails the build when |
|---|---|---|---|
| 1 | Checkout | Git (GitHub Branch Source) | — |
| 2 | Secrets scan | gitleaks `v8.30.1`, full Git history | Any secret found (TC-PIPE-01) |
| 3 | Lint and format | Spotless + Checkstyle (`backend`, `docs-api`); ESLint + Prettier (`frontend`) | Any violation |
| 4 | Code scan | Semgrep `1.187.0`, rule packs `p/java`, `p/typescript`, `p/dockerfile` | Any ERROR-severity (high) finding |
| 5 | Employee API tests | `./mvnw verify` in `eclipse-temurin:21.0.13_9-jdk-noble` (unit, matrix, Testcontainers, coverage gate) | Any test failure, or coverage below 70% |
| 6 | Document API tests | Same, for `docs-api` | Same |
| 7 | Frontend tests | Vitest in `node:22.23.5-alpine3.24` | Any failure |
| 8 | Dependency scan | Trivy `0.76.2` filesystem scan | High or critical with a fix available (TC-PIPE-02) |
| 9 | Build images | `docker build` for `securehr/api`, `securehr/docs-api`, `securehr/ui`, `securehr/edge` | Build error |
| 10 | Image scan | Trivy `0.76.2` image scan | Any high or critical |

**Areas that don't exist yet are skipped, not failed.** Lint and test stages run only when their
folder exists (`docs-api/pom.xml`, `frontend/package.json`). Images are built only for folders
that have a `Dockerfile`. When a teammate's folder is merged, its stages start gating
automatically, with no pipeline change. Stages 11–14 (staging deploy, Playwright, ZAP, promotion)
come in a later pull request, once the root Compose file and the edge are on `main`.

**Image tags:** the short commit ID. Builds of `main` also get the tag `main` (Section 0.11).

**Accepted vulnerabilities:** Trivy uses `backend/.trivyignore.yaml` for the filesystem scan.
The image scan uses each image folder's own `.trivyignore.yaml` when it has one. Every entry
gives a reason and an expiry date, and is owned by that folder's owner.

**Timeouts:** the whole run is limited to 60 minutes, and each stage has its own tighter limit.
A new push to a pull request aborts the build that's still running for it.

### Reports published on every build

| Report | Where in Jenkins |
|---|---|
| JUnit results (unit and integration) | Build page → Tests; the job page shows the test trend |
| Coverage (JaCoCo) | Build page → "Employee API coverage" summary and the HTML coverage report |
| gitleaks, Semgrep, Trivy (filesystem and image) | Build page → one Warnings entry per tool (SARIF), plus the raw files under Artifacts → `reports/` |

These are the test evidence for Section 13.5. Screenshots go in `/docs/evidence/ci/`.

## Jenkins configuration

### Sign-in (Section 0.5)
- **Keycloak only:** OIDC with the confidential client `jenkins`, authorization code + PKCE
  (S256). There is no local admin account.
- **Endpoints written out:** the endpoints are written out from `KEYCLOAK_ISSUER_URI`, not
  discovered, so Jenkins starts even when Keycloak isn't up yet.
- **Roles from the token:** roles come from `realm_access.roles`, and the username comes from
  `preferred_username`.
- **Deny by default:** realm role `SECUREHR_DEV` (directory group `securehr-devs`) is
  administrator. Everyone else gets no permission at all, not even read.
- **Logout:** logging out of Jenkins also ends the Keycloak session (TC-AUTH-04).

### GitHub (Section 11.4)
- **The job:** one multibranch pipeline job, `securehr`, over `jackkibort/tech-wreck-cmpe272`,
  running `ci/Jenkinsfile`.
- **Pull requests:** each pull request is built merged with `main`. A branch that also has a pull
  request is built once, as the pull request.
- **Commit status:** GitHub Branch Source reports each build as a commit status on the pull
  request. For pull requests the status context is `continuous-integration/jenkins/pr-merge`;
  branch protection on `main` requires it.
- **Webhooks:** the smee relay forwards GitHub webhooks to `http://jenkins:8080/github-webhook/`.
  If the relay is down, Jenkins rescans GitHub every 2 minutes.

### Environment variables (Section 0.9 names only)

| Variable | Used for |
|---|---|
| `SECUREHR_DOMAIN` | Jenkins URL `https://ci.${SECUREHR_DOMAIN}/` and the logout redirect (default `securehr.local`) |
| `KEYCLOAK_ISSUER_URI` | OIDC issuer and endpoints |
| `JENKINS_OIDC_CLIENT_SECRET` | Secret of the Keycloak client `jenkins` (from Vault `secret/securehr/clients/jenkins`) |
| `VAULT_ADDR` | Vault address (also used by `fetch-ci-secrets.sh`) |
| `GITHUB_WEBHOOK_PROXY_URL` | The smee.io channel URL (smee service) |

### CI credentials in Vault: `secret/securehr/ci`

| Key | Value |
|---|---|
| `github_username` | The GitHub account that owns the token |
| `github_token` | A fine-grained personal access token for the repository with **Contents: read**, **Commit statuses: read and write**, **Pull requests: read** and **Metadata: read** |

`scripts/fetch-ci-secrets.sh` writes these to `ci/secrets/` (git-ignored, owner-only). Compose
mounts them into Jenkins as Docker secrets, and configuration as code reads each file as a
variable named after it. The token never appears in an environment variable, a file in Git, the
image or the build log.

## How to run

You need Docker with the Compose plugin and the `vault` CLI. Run from the repository root.

```bash
# 1. Copy the GitHub credentials from Vault (log in with `vault login` first).
ci/scripts/fetch-ci-secrets.sh

# 2a. As part of the whole stack (the root Compose file includes ci/compose.yaml):
#     open https://ci.securehr.local and sign in with a securehr-devs account.

# 2b. On its own, before the edge exists (local development only):
docker compose -p securehr-prod --env-file .env -f ci/compose.yaml -f ci/compose.local.yaml up -d --build
#     Jenkins is then at http://localhost:8090. Sign-in still goes through Keycloak.
```

**Set up GitHub once (repository settings):**
1. Create a channel at https://smee.io and put its URL in `.env` as `GITHUB_WEBHOOK_PROXY_URL`.
2. In the repository settings, add a webhook:
   - **Payload URL:** the smee URL.
   - **Content type:** `application/json`.
   - **Events:** pushes and pull requests.
3. Turn on branch protection for `main`:
   - Require a pull request with 1 approval and review from code owners.
   - Require the status check `continuous-integration/jenkins/pr-merge`.
   - Block force pushes and deletions.

## How to test

**These checks were run on Nov 24, 2026, against a local Jenkins built from this folder:**
- **Configuration loads:** Jenkins starts with `security.yaml`, `credentials.yaml` and `jobs.yaml`.
  It creates the `securehr` job and the `github-token` credential.
- **Access is denied by default:** an anonymous request gets 403. The login link redirects to
  `https://auth.securehr.local/realms/securehr/protocol/openid-connect/auth` with
  `client_id=jenkins` and `code_challenge_method=S256`.
- **Green build:** a full pipeline run on this branch passed all 10 stages.
  - All 88 Employee API tests ran (26 unit, 62 integration with Testcontainers).
  - `securehr/api:<commit>` was built and passed the image scan with 0 high or critical findings.
  - The run used a test-only local login, not part of this folder.
- **TC-PIPE-01:** a commit containing a fake AWS key failed at stage 2, and no later stage ran.

**Test locally without Keycloak:** start Jenkins with an extra Compose file that points
`CASC_JENKINS_CONFIG` at a folder with a local test user and a pipeline job. Keep that file
outside the repository and never commit a local login.

## Design decisions

| Decision | Why |
|---|---|
| **Every tool in its own pinned container** (Docker Pipeline plugin) | Tool versions live in one place; the Jenkins image stays small; builds are the same on every machine |
| **Jenkins uses the host Docker engine** (known trade-off, Section 11.4) | Needed to run tool containers, Testcontainers and image builds on one machine. Access to the Docker socket is equivalent to root on the host. A production setup would use separate build agents with no socket access. |
| **Socket group read at start** (`docker-socket-entrypoint.sh`) | The socket's group is `docker` on Linux and `root` on Docker Desktop. Reading it at start works on both without a host-specific setting. Jenkins itself never runs as root. |
| **OIDC endpoints written out, not discovered** | Jenkins starts before Keycloak and recovers on its own once Keycloak is up |
| **Secrets scan covers the full Git history** | A secret that was committed and then deleted is still leaked and must be rotated |
| **Missing areas are skipped, not failed** | The pipeline is useful from day one and needs no edits as each folder lands |
| **The Maven wrapper is run as `sh ./mvnw`** | The pipeline doesn't depend on the file's executable bit in Git |
| **Trivy reads the Jenkins Maven cache** | Maven Central rate-limits repeated pom downloads from one address. The test stages already fill the cache, so stage 8 reuses it. |
| **The Jenkins and smee images are unnamed** | Section 0.11 reserves the `securehr/*` names for the application images; Compose names these after the project |

### Design patterns (Section 12.4)

| Pattern | Where |
|---|---|
| Pipeline | `Jenkinsfile`: ordered stages, each a gate; the first failure stops the run |
| Configuration as code | `jenkins/casc/*`: Jenkins rebuilt from files, never configured by hand |
| Identity federation | Jenkins trusts Keycloak (OIDC) instead of keeping its own users |

## Versions

| Component | Version |
|---|---|
| Jenkins | `jenkins/jenkins:2.580.2-lts-jdk21` |
| Docker CLI (in the Jenkins image) | `docker:29.10.1-cli` |
| smee client | `smee-client@5.0.0` on `node:22.23.5-alpine3.24` |
| gitleaks | `zricethezav/gitleaks:v8.30.1` |
| Semgrep | `semgrep/semgrep:1.187.0` |
| Trivy | `aquasec/trivy:0.76.2` |
| Plugins | See `jenkins/plugins.txt` |
