# /observability: logs, dashboards and alerts (owner: Samhitha)

Every service's standard output is collected, stored and shown on dashboards, and the security
alerts in Section 11.5 of the implementation plan are evaluated against those logs. Everything is
set up from the files in this folder; nothing is clicked together in the Grafana UI.

```text
all containers ─stdout─▶ alloy ─▶ loki ◀─ grafana (dashboards, alerts) ◀─ edge ◀─ browser
```

## What's here

| Path | Purpose |
|---|---|
| `compose.yaml` | Area Compose file: `loki`, `alloy`, `grafana` (Section 0.2) |
| `loki/loki.yaml` | Single-binary Loki on local disk; 30-day retention |
| `alloy/config.alloy` | Finds the SecureHR containers and ships their logs with the two allowed labels |
| `grafana/grafana.ini` | Keycloak-only sign-in and the role mapping |
| `grafana/provisioning/` | The Loki data source, the dashboard folders and the alert rules |
| `grafana/dashboards/Audit/` | Security overview; Sensitive data access (open to auditors) |
| `grafana/dashboards/Platform/` | Platform health and the SLO (developers only) |
| `scripts/apply-folder-permissions.sh` | Closes the Platform folder to Viewers (auditors) |
| `SLO.md` | The directory-search SLO and its error budget |

## Labels (Section 0.10)

Loki streams have exactly two labels:

| Label | Value |
|---|---|
| `service` | The Compose service name (`api`, `docs-api`, `edge`, `keycloak`, …). Staging services are prefixed `staging-` so they never mix with production |
| `log_type` | `audit` when the JSON line says so, otherwise `app` (including services that don't log JSON) |

**No personal data in labels:** usernames, emp_no values and request IDs stay inside the log
line, never in labels. Queries read them with `| json`. Loki 3's automatic `service_name` label is
turned off, so it can't add a third label.

**Which containers:** only containers of the Compose projects `securehr-prod` and
`securehr-staging` are collected.

## Dashboards (Section 11.5)

| Folder / dashboard | Panels |
|---|---|
| **Audit / Security overview** | Totals for login failures, 403s, firewall blocks, rate-limit hits and lockouts; logins success vs. failure over time; firewall and rate-limit blocks over time; top users by 403s and by login failures; top source IPs by firewall blocks; latest security events |
| **Audit / Sensitive data access** | Totals for salary reads, exports and document uploads, downloads and deletes; these over time; top users; latest events with actor, target emp_no and request ID (G4) |
| **Platform / Platform health** | Services logging in the last 5 minutes; API 5xx share; edge p50/p95 response time; the SLO ratio and error budget left; recent errors |

## Alerts (Section 11.5)

| Rule | Query | Fires when |
|---|---|---|
| More than 10 access denials from one user in 1 minute | `ACCESS_DENIED` count by `actor_username` over 1 minute | > 10 |
| More than 5 login failures for one username in 5 minutes | `LOGIN_FAILURE` count by `actor_username` over 5 minutes | > 5 |
| Export or document delete outside 8am–6pm | `EXPORT` or `DOC_DELETE` whose own timestamp is outside business hours | > 0 |

**Business hours are written in UTC.** LogQL can't convert time zones, so 8am–6pm PST (UTC−8)
is 16:00–02:00 UTC. During daylight saving time, change the hours in `rules.yaml` from 2 and 16
to 1 and 15.

**Where alerts show:** in Grafana under Alerting → Alert rules. No notification channel is set
up for the proof of concept.

## Sign-in and roles (Section 0.5)

- **Keycloak only:** confidential client `grafana`, authorization code + PKCE. The login form,
  basic auth and anonymous access are all off.
- **Role mapping:**
  - `SECUREHR_DEV` → Grafana server admin.
  - `AUDITOR` → Viewer.
  - Anyone else has no role, so Grafana refuses the login (strict role mapping).
- **Audit folder only for auditors:** Grafana's file provisioning can't set folder permissions.
  After the first start, a developer runs `scripts/apply-folder-permissions.sh` once, which
  removes Viewer access from the Platform folder.

## Configuration (Section 0.9 names only)

| Variable | Used by | Purpose |
|---|---|---|
| `LOKI_URL` | alloy, grafana | Loki address (default `http://loki:3100`) |
| `KEYCLOAK_ISSUER_URI` | grafana | OIDC endpoints |
| `GRAFANA_OIDC_CLIENT_SECRET` | grafana | Secret of the Keycloak client `grafana` (Vault `secret/securehr/clients/grafana`) |
| `SECUREHR_DOMAIN` | grafana | Public URL `https://grafana.${SECUREHR_DOMAIN}/` |

## How to run

The root Compose file includes this file. To run only this area, from the repository root:

```bash
docker compose -p securehr-prod --env-file .env -f observability/compose.yaml up -d
```

**One-time step:** after Grafana's first start, a `securehr-devs` user creates an Admin service
account token in Grafana. Then:

```bash
GRAFANA_TOKEN='<token>' observability/scripts/apply-folder-permissions.sh
```

## How to test

**These checks were run on Nov 28, 2026, with a test container writing audit lines in the
Section 11.1 format:**
- **Labels:** the streams had exactly `service` and `log_type`, with values `api`/`audit` and
  `api`/`app`, and `loki`, `alloy`, `grafana` as `app`.
- **Dashboards:** all 3 were loaded into the right folders. Every panel query ran without
  error, and the audit panels showed the test events.
- **Alerts:** all 3 fired, naming the right user and event:
  - 12 denials in a minute for `demo.employee`;
  - 6 login failures for `demo.employee2`;
  - an `EXPORT` stamped 3 am Pacific.
- **Folder permissions:** after `apply-folder-permissions.sh`, a Viewer saw only the 2 Audit
  dashboards.

**Rerun it yourself:** start this area with an extra Compose file that adds a log-generator
container to the `securehr-prod` project and turns on a local Grafana login. Keep that file out
of the repository.

## Needed from other areas

| From | What |
|---|---|
| Jack | Keycloak client `grafana` (`https://grafana.securehr.local/*`); its secret in Vault. Keycloak login events reaching the logs: as audit lines with `event_type` `LOGIN_SUCCESS` / `LOGIN_FAILURE`, or Keycloak's own event log, where lockouts show as `user_temporarily_disabled` |
| Arthi | Edge route `grafana.securehr.local` → `grafana:3000`. JSON access logs with `uri`, `status` and `request_time` (seconds), which the platform and SLO panels read; firewall and rate-limit events as audit lines with `event_type` `WAF_BLOCK` / `RATE_LIMITED`. Grafana must trust the mkcert certificate to reach Keycloak |

## Design decisions

| Decision | Why |
|---|---|
| Loki + Alloy instead of ELK | Much lighter on memory on one machine (Section 4.3) |
| Logs read from the Docker engine, not files | Every service already logs to stdout (Section 0.10); no service needs changing |
| `log_type` read from the JSON line | The APIs already mark audit lines; everything else is `app` by default |
| Platform folder closed by a script | Grafana's file provisioning can't set folder permissions; the script is the smallest step that can |
| No Loki healthcheck | The image has no shell or HTTP client; Alloy and Grafana retry until Loki is ready |

### Design patterns (Section 12.4)

| Pattern | Where |
|---|---|
| Observer / event listener | Services emit events to stdout; Alloy observes and ships them without the services knowing |
| Pipes and filters | Alloy: discover → relabel → parse `log_type` → write |
| Configuration as code | Dashboards, data source and alert rules provisioned from files |

## Versions

| Component | Image |
|---|---|
| Loki | `grafana/loki:3.7.11` |
| Alloy | `grafana/alloy:v1.22.0` |
| Grafana | `grafana/grafana:13.3.2` |
