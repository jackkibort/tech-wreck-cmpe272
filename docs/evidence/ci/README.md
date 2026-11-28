# CI evidence (owner: Samhitha)

Screenshots of the Jenkins pipeline (Section 13.5 of the implementation plan). File names start
with the test case ID where there is one.

## How these were made

- **When:** Nov 28, 2026.
- **Jenkins:** a local controller built from `/ci`: the same image, plugins and `ci/Jenkinsfile`
  as the real one.
- **Two test-only differences:**
  - **Sign-in:** a local login instead of Keycloak, since the Keycloak realm isn't on `main` yet.
  - **Source:** a multibranch job over a local copy of the repository instead of GitHub, since
    the GitHub token isn't in Vault yet.
- **Branches built:**
  - `main` at commit `9e99636`, plus the two pipeline fixes in this pull request.
  - `demo/vulnerable-dependency`.
  - `test/tc-pipe-01`, a branch with one commit adding a fake AWS key. It exists only in that
    local copy and was never pushed.

## What each screenshot shows

| File | Shows |
|---|---|
| `TC-PIPE-01-gitleaks-blocked-pipeline.png` | The fake-key branch fails at **stage 2 (Secrets scan)**: gitleaks reports 2 findings (the key ID and secret), and nothing after stage 2 runs |
| `TC-PIPE-01-gitleaks-blocked-console.png` | The console log of that build, with gitleaks' redacted output |
| `TC-PIPE-02-trivy-blocked-pipeline.png` | `demo/vulnerable-dependency` passes stages 1–7, then fails at **stage 8 (Dependency scan)** on `lodash` 4.17.20: CVE-2021-23337 (HIGH, fixed in 4.17.21) |
| `TC-PIPE-02-trivy-blocked-findings.png` | The Trivy findings published on that build (2 high, both with fixes) |
| `main-test-results-88-passing.png` | `main`: all 88 Employee API tests pass in Jenkins (26 unit, 62 integration with Testcontainers) |
| `main-coverage.png` | The JaCoCo coverage published from the same build |
| `main-stages-1-8-pass-stage-9-blocked-by-mvnw.png` | `main` passes stages 1–8 and stops at **stage 9 (Build images)**: `backend/mvnw` isn't executable in Git, so the API image can't build from a clean checkout |
| `jenkins-branches.png` | The multibranch job with each branch's result |

## Not yet captured

| Evidence | Waiting on |
|---|---|
| A fully green `main` pipeline and the Jenkins test-trend page | Sabari: `git update-index --chmod=+x backend/mvnw` |
| Stages 11–14: staging deploy, Playwright report, ZAP report, promotion | Arthi's root Compose file and edge, and Jack's Keycloak, LLDAP and Vault on `main` |
| Grafana dashboards with live events | The full stack running |
| k6 results table | The full stack running (`perf/run.sh`) |

## Bugs these runs found, fixed in the same pull request

**Secrets scan failing unrelated branches.** gitleaks scanned every branch the checkout fetched,
so the fake key on `test/tc-pipe-01` failed `main` and the demo branch at stage 2 too. The scan is
now limited to the history of the commit being built (`--log-opts=HEAD`): all of `main`'s 30
commits are still scanned.

**Trivy crash with builds running at the same time.** Two builds shared the Trivy database while
one of them was updating it, and Trivy crashed. Updates now take an exclusive lock and scans a
shared one. Verified by running `main` and the demo branch side by side.
