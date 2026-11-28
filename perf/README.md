# /perf: load and burst tests (owner: Samhitha)

k6 tests for two of the plan's targets (Sections 2 and 13):
- **G3:** directory search over the 300,000-employee dataset keeps its 95th-percentile response
  time **under 300 ms**, with the firewall on.
- **TC-SEC-03:** a burst of requests beyond the edge's per-IP limit gets **429** responses, and
  the API returns no 5xx.

## What's here

| Path | Purpose |
|---|---|
| `scenarios/search.js` | G3: three search mixes (name prefix, department, plain paging) for 60 s; fails if any p95 ≥ 300 ms or any request isn't 200 |
| `scenarios/burst.js` | TC-SEC-03: 200 requests a second for 15 s from one address; fails if no 429 is seen or any 5xx is |
| `lib/auth.js` | Gets the access token once, in setup, so login time is never measured |
| `lib/summary.js` | Writes `results/<test>.json` (raw k6 summary) and `results/<test>.md` (the table for the report) |
| `run.sh` | Runs a test in the pinned k6 container against the running stack |
| `results/` | Saved results from the official runs |

The search mix is the same as the Employee API's own check (`backend/perf-check`), so the numbers
are comparable.

## How to run

You need the `securehr-prod` stack running with the full dataset imported, and `mkcert`
installed: k6 trusts the local certificate authority, so no certificate check is skipped.

Sign in as `demo.employee`. The test signs in the way the portal does: authorization code + PKCE
with the `securehr-portal` client. Use an account without MFA. Put the password, read from Vault,
in your shell for the run only:

```bash
read -rs SECUREHR_PASSWORD && export SECUREHR_PASSWORD
```

```bash
perf/run.sh search-edge      # G3 through the edge: TLS, firewall and rate limit on
```

```bash
perf/run.sh search-direct    # the same searches straight to api:8080: no edge, no firewall
```

```bash
perf/run.sh burst            # TC-SEC-03 against the edge
```

**Other options:**
- **`ACCESS_TOKEN` instead of a password:** if you already have a token, set `ACCESS_TOKEN` and
  the test skips the sign-in.
- **`DURATION`:** changes how long the search test runs (default `60s`).

**Running the search tests back to back:** the edge rate limit applies to the load test too. If
`search-edge` gets 429s, wait a minute between runs. If it still does, ask Arthi for the limit
and lower the virtual users in `scenarios/search.js`.

## Results

Each run prints a table and saves it to `results/`. Copy the two search tables into the table
below for the report (Section 13.5): median and p95, firewall on vs. off.

| Run | Scenario | Median (ms) | p95 (ms) | G3 (< 300 ms) |
|---|---|---|---|---|
| Through the edge (firewall on) | name | — | — | — |
| | department | — | — | — |
| | directory | — | — | — |
| Direct to the API (no firewall) | name | — | — | — |
| | department | — | — | — |
| | directory | — | — | — |

**Burst (TC-SEC-03):** to be filled in from `results/burst.md` (429 count; 5xx must be 0).

**Not filled in yet:** the official runs need the full stack (Keycloak, edge, API, dataset). For
reference, Sabari's local run straight to the API on the full dataset is recorded in
`backend/README.md`.

## How the scripts were tested

On Nov 28, 2026, both scenarios ran against a mock API and a mock Keycloak login (test-only,
not in the repository):
- **Sign-in works:** the PKCE login got a token, which the search used.
- **Results are saved:** the tables and JSON files were written.
- **The burst gate works:** it counted 429s once the mock's limit was passed.
- **A failed login fails the run:** a wrong password stopped the run with a clear error, and the
  table showed FAIL.

## Design decisions

| Decision | Why |
|---|---|
| **Sign in as the portal does** (code + PKCE, `securehr-portal`) | No extra Keycloak client or password grant is needed, and the token is a real portal token with the real audiences and roles |
| **The token is fetched once, in setup** | Login cost isn't part of the search numbers |
| **Every request has a 5 s timeout** | A hung request counts as a failure instead of stretching the run |
| **"Firewall off" means straight to the API** | It needs no second edge configuration, and the gap between the two runs is the whole cost of the edge (TLS, firewall, rate limit) |
| **Secrets are passed by name** (`-e NAME`) | Docker reads them from your shell, so they never appear in the process list or a file |
| **No remote k6 libraries** | A test run never downloads code |

## Versions

| Component | Image |
|---|---|
| k6 | `grafana/k6:2.4.0` |
