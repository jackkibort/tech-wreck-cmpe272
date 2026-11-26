# Local TLS certificates (owner: Arthi)

Every SecureHR hostname is served over HTTPS by the edge with **one** certificate made by
[mkcert](https://github.com/FiloSottile/mkcert). mkcert creates a small certificate authority
(CA) on your machine and adds it to your system and browser trust stores, so the portal opens
with no certificate warning. Each teammate does this once on their own machine; certificates
and keys are never committed (`edge/certs/` is git-ignored).

## Hostnames covered

| Hostname | Used for |
|---|---|
| `securehr.local` | Portal and both APIs |
| `auth.securehr.local` | Keycloak |
| `ci.securehr.local` | Jenkins |
| `grafana.securehr.local` | Grafana |
| `directory.securehr.local` | LLDAP admin UI |
| `staging.` + each of the above | Staging stack on port 8443 (Section 11.3) |

## Step 1: install mkcert

| OS | Command |
|---|---|
| macOS | `brew install mkcert nss` (`nss` is only needed for Firefox) |
| Windows | `choco install mkcert` or `scoop install mkcert` |
| Linux | `sudo apt install libnss3-tools` then download the `mkcert` binary from its GitHub releases page |

## Step 2: create and trust the local CA (once per machine)

```bash
mkcert -install
```

Your OS may ask for your password: this adds the mkcert CA to the system trust store.
Restart the browser afterwards. **Never share the `rootCA-key.pem` file** in the folder shown by
`mkcert -CAROOT`; anyone holding it can issue certificates your machine trusts.

## Step 3: generate the SecureHR certificate

From the repository root:

```bash
edge/scripts/make-certs.sh
```

This writes `edge/certs/securehr.pem`, `edge/certs/securehr-key.pem` and a copy of the CA
certificate `edge/certs/rootCA.pem` (public part only). Containers that call
`https://auth.securehr.local` from inside Docker (the APIs, Grafana, Jenkins) mount
`rootCA.pem` so they trust the edge.

## Step 4: point the hostnames at your machine

Add this line to your hosts file (`/etc/hosts` on macOS/Linux,
`C:\Windows\System32\drivers\etc\hosts` on Windows, edited as administrator):

```text
127.0.0.1 securehr.local auth.securehr.local ci.securehr.local grafana.securehr.local directory.securehr.local staging.securehr.local staging.auth.securehr.local staging.ci.securehr.local staging.grafana.securehr.local staging.directory.securehr.local
```

Inside Docker no hosts file is needed: the edge service carries these names as network aliases.

## Check

After the stack is running (root README), open <https://securehr.local>. The browser shows a
padlock and no warning. From a terminal:

```bash
curl -sI https://securehr.local | head -1
```

`curl` uses the system trust store, so it succeeds without `-k`.

## Troubleshooting

| Symptom | Fix |
|---|---|
| Browser warns "not private" | Run `mkcert -install` again and restart the browser. Firefox needs `nss` (step 1). |
| `ERR_NAME_NOT_RESOLVED` | The hosts file line from step 4 is missing. |
| Edge exits with "cannot load certificate" | Run step 3; check that `edge/certs/` holds three `.pem` files. |
| A container fails TLS to `auth.securehr.local` | It is not mounting `edge/certs/rootCA.pem`; see that service's README. |
