# /edge: NGINX + ModSecurity edge (owner: Arthi)

The edge is the only service that publishes host ports. It terminates TLS, applies the web
application firewall (ModSecurity v3 + OWASP Core Rule Set) and per-IP rate limits, sets
security headers and the request ID, and routes each hostname and path to its service
(Sections 0.3 and 8.4). Design pattern: **Gateway / reverse proxy**.

## Contents

| Path | Purpose |
|---|---|
| `Dockerfile` | `securehr/edge` image: the official OWASP CRS NGINX image plus our configuration |
| `compose.yaml` | Area file: the `edge` service (included by `deploy/compose/compose.yaml`) |
| `compose.staging.yaml` | Staging override: `staging.` hostnames on host port 8443 |
| `templates/conf.d/default.conf.template` | Server blocks, routing, TLS, rate limits, request ID |
| `templates/conf.d/logging.conf.template` | JSON access log; tags `WAF_BLOCK` and `RATE_LIMITED` audit events |
| `templates/includes/*.template` | Security headers, portal CSP, JSON error bodies |
| `modsecurity/REQUEST-900-EXCLUSION-RULES-BEFORE-CRS.conf` | Documented firewall rule exceptions |
| `waf-tests/payloads.tsv`, `scripts/waf-test.sh` | TC-SEC-01 payload list and runner |
| `CERTIFICATES.md`, `scripts/make-certs.sh` | Local TLS with mkcert |

## Pinned versions

| Component | Version |
|---|---|
| Base image | `owasp/modsecurity-crs:4.30.0-nginx-alpine-202611190211` (CRS 4.30.0, NGINX, Alpine) |

## Routing (Section 0.3)

| Hostname | Path | Upstream | Notes |
|---|---|---|---|
| securehr.local | `/` | `ui:8080` | |
| securehr.local | `/api/documents…` | `docs-api:8080` | Body limit 10 MB; uploads rate-limited |
| securehr.local | `/api/export/…` | `api:8080` | Stricter rate limit |
| securehr.local | `/api/…` | `api:8080` | |
| securehr.local | `/api/health`, `/api/documents/health` | none | 404; internal only |
| auth.securehr.local | `/` | `keycloak:8080` | Login form posts rate-limited |
| ci.securehr.local | `/` | `jenkins:8080` | |
| grafana.securehr.local | `/` | `grafana:3000` | |
| directory.securehr.local | `/` | `ldap:17170` | |
| any other name | `/` | none | 421 |

Port 80 redirects everything to HTTPS. Upstream names are resolved per request through Docker's
DNS, so the edge starts even when a service is not up yet (that route answers 503 until it is).

## Security settings (Section 8.4)

| Setting | Value |
|---|---|
| TLS | 1.2 and 1.3 only; Mozilla "intermediate" ciphers; HSTS one year |
| Headers on every response | `Strict-Transport-Security`, `X-Content-Type-Options`, `Referrer-Policy`, `Permissions-Policy`, `X-Request-ID` |
| Portal and API only | `Content-Security-Policy` (self + Keycloak origin), `X-Frame-Options: DENY` |
| Server header | Removed entirely |
| Firewall | ModSecurity on, blocking, paranoia level 1, anomaly threshold 5 (CRS default) |
| Rate limits (per IP) | API 20 r/s (burst 40); export, login posts, uploads 10 r/min (burst 5–10); web 50 r/s |
| Body size | 1 MB default; 10 MB on `/api/documents` |
| X-Forwarded-For | Overwritten with the client address, never appended (the edge is the first hop) |
| X-Request-ID | Kept if it matches `[A-Za-z0-9._-]{8,64}`, otherwise generated |

Errors the edge produces itself on the portal host (403 firewall block, 413, 429, 503) use the
APIs' JSON error format (`status`, `error`, `message`, `requestId`), so the UI handles them the
same way. 429 responses carry `Retry-After`.

## Firewall rule exceptions

| ID | Scope | Reason |
|---|---|---|
| 1000 | `ARGS:password*` on Keycloak `/realms/*/login-actions/` only | Strong passwords trip SQL injection rules and would block correct logins (and count toward lockout). Keycloak only binds the password against LDAP. Brute force is covered by rate limits and Keycloak lockout. |

Add new exceptions only with a reason, as narrow as possible, and list them here.

## Logs (Sections 0.10 and 11.1)

- Access log: one JSON object per line on stdout. Ordinary requests have `log_type: app`.
  Responses the edge generated itself are audit events: 403 → `event_type: WAF_BLOCK`,
  429 → `event_type: RATE_LIMITED`, both with `outcome: DENIED`, plus `request_id`,
  `source_ip` and `resource`.
- Paths are logged without query strings, so OIDC authorization codes and search terms never
  reach the logs.
- ModSecurity's audit log (the matched rules) is JSON on stdout; NGINX errors go to stderr.

## How to run

1. Create the certificates: [CERTIFICATES.md](CERTIFICATES.md).
2. Start the stack from the repository root (see `deploy/compose/README.md`), or only the edge:

   ```bash
   docker compose -p securehr-prod --env-file .env -f edge/compose.yaml up -d --build
   ```

3. Staging edge (Section 11.3):

   ```bash
   docker compose -p securehr-staging --env-file .env -f deploy/compose/compose.yaml -f edge/compose.staging.yaml up -d edge
   ```

## How to test

| Check | Command |
|---|---|
| TC-SEC-01 firewall payloads | `edge/scripts/waf-test.sh` (all `PASS`, exit 0) |
| Security headers | `curl -sI https://securehr.local/` |
| HTTP → HTTPS | `curl -sI http://securehr.local/` (301) |
| TLS 1.1 refused | `curl -sI --tls-max 1.1 https://securehr.local/` (handshake fails) |
| Rate limit | `for i in $(seq 1 10); do curl -s -o /dev/null -w '%{http_code} ' https://securehr.local/api/export/employees; done` (ends in 429) |
| Upload limit | `head -c 11000000 /dev/zero | curl -s -X POST --data-binary @- https://securehr.local/api/documents` (413) |

## Design decisions

- **Ports 443/80 inside the container.** The APIs fetch Keycloak's keys from
  `https://auth.securehr.local`, which must reach the edge on the internal network at port 443.
  The image's unprivileged user normally cannot bind ports below 1024, so the Compose file sets
  the namespaced sysctl `net.ipv4.ip_unprivileged_port_start=0` for this container only, and the
  Dockerfile removes the image's low-port check. The container still runs as non-root, with all
  capabilities dropped.
- **Network aliases** make the public hostnames resolve to the edge inside Docker, so tokens are
  validated against the same issuer URL the browser sees.
- **Our own server blocks** replace the image's single-backend template; the image's
  `modsecurity on` include and CRS setup are kept unchanged.
- **Responses the edge generated are the security events.** A 403 or 429 with no upstream
  status can only come from the firewall or the rate limiter, which makes `WAF_BLOCK` and
  `RATE_LIMITED` reliable without parsing the ModSecurity log.
- **Refresh tokens, not a silent-renew iframe**, so the portal CSP can forbid all framing.

## Needs from other areas

- **api / docs-api / grafana / jenkins:** containers that call `https://auth.securehr.local`
  must trust the mkcert CA. Mount `edge/certs/rootCA.pem` read-only and add it to the trust
  store (for the JVM, a truststore built from it or `-Djavax.net.ssl.trustStore=...`).
- **keycloak:** run behind the proxy with `KC_PROXY_HEADERS=xforwarded` and hostname
  `https://auth.securehr.local`; the edge sends `X-Forwarded-Proto/Host/Port/For`.
