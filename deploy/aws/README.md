# /deploy/aws: running SecureHR on AWS (owner: Samhitha)

The graded demo runs on Docker Compose on one machine. This guide puts the same runtime stack
on one EC2 instance, for a public deployment if one is needed (Section 19 of the implementation
plan). Nothing in this folder runs automatically. Each step is a command a teammate runs by
hand, with a way to check it worked.

> **Prices and free-plan rules change.** Check the
> [EC2 pricing page](https://aws.amazon.com/ec2/pricing/on-demand/) and your account's Free plan
> page before you start. The figures below are estimates for planning, not quotes.

## What runs where

| Where | Services |
|---|---|
| **EC2 instance** | edge, ui, api, docs-api, mysql, docs-db, keycloak, keycloak-db, ldap, vault, alloy, loki, grafana |
| **Stays local** | jenkins, smee, and the staging stack (`securehr-staging`) |

```text
Browser ─HTTPS 443─▶ Elastic IP ─▶ EC2 (security group: 80, 443 only)
                                    └─ Docker Compose: securehr-prod (edge is the only published service)
Teammates ─▶ AWS Systems Manager Session Manager ─▶ EC2 shell (no SSH port open)
Jenkins (local, main only) ─push images─▶ GitHub Container Registry ◀─pull─ EC2
```

## Cost (estimates, us-west-2, on-demand, Nov 2026)

| Item | Choice | Rough monthly cost if left running |
|---|---|---|
| Instance | 2 vCPU, **8 GiB** RAM (for example `t3.large`, or an 8 GiB type your Free plan allows) | about $60 |
| Disk | 30 GB gp3 | about $2.50 |
| Public IPv4 (Elastic IP) | 1 address | about $3.60 |
| Snapshots | 1 after the first deploy | about $1.50 |

**The stack needs at least 8 GiB of RAM.** Keycloak, two Spring Boot APIs, MySQL with the
300,000-employee dataset, and Loki don't fit in less.

**How to keep it cheap:**
- **New-account credits:** accounts created on or after Jul 15, 2025 get up to $200 in credits
  and a Free plan lasting up to 6 months. Only some small instance types are allowed on the Free
  plan, so check which of them have 8 GiB or more.
- **Stop it when idle:** a stopped instance costs only its disk and its Elastic IP.
- **Ask about AWS Academy or AWS Educate:** check with the professor first. Through the
  university, that's usually the truly free route.

## Steps

All commands use the AWS CLI v2, run from your laptop unless a step says "on the instance".
Set the region once:

```bash
export AWS_REGION=us-west-2
```

### 1. Account safety (once, in the console)
1. Turn on MFA for the root user. After that, don't use the root user.
2. In IAM Identity Center, create one user per teammate and give each the
   `AdministratorAccess` permission set for this account. Then sign in with
   `aws configure sso`.
3. In Billing → Budgets, create a monthly cost budget with email alerts at **$10, $25 and $50**.

**Check:** `aws sts get-caller-identity` shows your Identity Center role, not `root`.

### 2. Network: security group with 80 and 443 only

```bash
VPC_ID=$(aws ec2 describe-vpcs --filters Name=isDefault,Values=true --query 'Vpcs[0].VpcId' --output text)
SG_ID=$(aws ec2 create-security-group --group-name securehr-web \
  --description "SecureHR: HTTPS and HTTP redirect only" --vpc-id "$VPC_ID" --query GroupId --output text)
aws ec2 authorize-security-group-ingress --group-id "$SG_ID" --protocol tcp --port 443 --cidr 0.0.0.0/0
aws ec2 authorize-security-group-ingress --group-id "$SG_ID" --protocol tcp --port 80 --cidr 0.0.0.0/0
```

**No SSH:** port 22 is never opened. Shell access goes through Session Manager (step 3).

**Check:** `aws ec2 describe-security-groups --group-ids "$SG_ID"` lists only 80 and 443.

### 3. IAM role for Session Manager

```bash
aws iam create-role --role-name securehr-ec2 --assume-role-policy-document \
  '{"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"Service":"ec2.amazonaws.com"},"Action":"sts:AssumeRole"}]}'
aws iam attach-role-policy --role-name securehr-ec2 \
  --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore
aws iam create-instance-profile --instance-profile-name securehr-ec2
aws iam add-role-to-instance-profile --instance-profile-name securehr-ec2 --role-name securehr-ec2
```

### 4. Instance

Ubuntu 24.04 LTS, 8 GiB RAM, 30 GB encrypted gp3 disk, and IMDSv2 required. The instance
metadata service only answers signed requests, which blocks a common way of stealing credentials.

```bash
AMI_ID=$(aws ssm get-parameter --name \
  /aws/service/canonical/ubuntu/server/24.04/stable/current/amd64/hvm/ebs-gp3/ami-id \
  --query Parameter.Value --output text)
INSTANCE_ID=$(aws ec2 run-instances --image-id "$AMI_ID" --instance-type t3.large \
  --security-group-ids "$SG_ID" --iam-instance-profile Name=securehr-ec2 \
  --metadata-options HttpTokens=required,HttpEndpoint=enabled \
  --block-device-mappings 'DeviceName=/dev/sda1,Ebs={VolumeSize=30,VolumeType=gp3,Encrypted=true}' \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=securehr}]' \
  --query 'Instances[0].InstanceId' --output text)
aws ec2 wait instance-running --instance-ids "$INSTANCE_ID"
```

**Check:** after a minute or two,
`aws ssm describe-instance-information --query 'InstanceInformationList[].InstanceId'` lists
the instance.

### 5. Static address

```bash
ALLOC_ID=$(aws ec2 allocate-address --query AllocationId --output text)
aws ec2 associate-address --instance-id "$INSTANCE_ID" --allocation-id "$ALLOC_ID"
PUBLIC_IP=$(aws ec2 describe-addresses --allocation-ids "$ALLOC_ID" --query 'Addresses[0].PublicIp' --output text)
echo "$PUBLIC_IP"
```

### 6. Hostnames and TLS

The local names (`securehr.local`, …) don't resolve on the internet, so the public deployment
needs real hostnames. Pick one:

| Option | Hostnames |
|---|---|
| **A domain you own** (cheapest is a few dollars a year) | A records for `securehr.<domain>`, `auth.securehr.<domain>`, `grafana.securehr.<domain>` → `$PUBLIC_IP` |
| **sslip.io** (free, no setup) | `securehr.<ip-with-dashes>.sslip.io`, `auth.securehr.<…>.sslip.io`, `grafana.securehr.<…>.sslip.io` |

**Hostnames:**
- **Set the domain:** set `SECUREHR_DOMAIN` in the instance's `.env` to the new base name. Every
  service builds its URLs from it (Section 0.9), so no file changes.
- **Not public:** `ci.` and `directory.` aren't exposed on AWS. Jenkins stays local, and the
  directory admin UI is reached only through Session Manager port forwarding (step 10).

**Certificates:** use Let's Encrypt instead of mkcert. On the instance, before the stack starts,
with port 80 free:

```bash
sudo apt-get install -y certbot
sudo certbot certonly --standalone --agree-tos -m <team-email> \
  -d "securehr.$DOMAIN" -d "auth.securehr.$DOMAIN" -d "grafana.securehr.$DOMAIN"
```

**Use the certificate:** copy the result into the edge's certificate folder, using the file
names the edge expects (see `/edge/README.md`). Renewal runs from certbot's timer; reload the
edge after each renewal.

**Check:** `curl -I https://securehr.$DOMAIN` returns the portal with a trusted certificate.

### 7. Keycloak for the public hostname (Jack)
1. **Hostname:** set Keycloak's public hostname to `https://auth.securehr.$DOMAIN`.
2. **Redirect URIs:** add the new redirect URIs and web origins to the clients:
   - `securehr-portal`: `https://securehr.$DOMAIN/*`;
   - `grafana`: `https://grafana.securehr.$DOMAIN/*`.
3. **GitHub:** register the production callback URL in the GitHub OAuth app:
   `https://auth.securehr.$DOMAIN/realms/securehr/broker/github/endpoint`.

**Check:** the issuer in `https://auth.securehr.$DOMAIN/realms/securehr/.well-known/openid-configuration`
matches `KEYCLOAK_ISSUER_URI` in `.env`.

### 8. Images through GitHub Container Registry
1. **Jenkins pushes (main builds only):** add a step to Jenkins that tags each image as
   `ghcr.io/<owner>/securehr-api:<tag>` (and the same for `docs-api`, `ui`, `edge`), then pushes
   it. The registry token is stored in Jenkins (from Vault `secret/securehr/ci`), never in the
   Jenkinsfile.
2. **The instance pulls:** on the instance, log in with a read-only token
   (`docker login ghcr.io`), then pull.
3. **Same commit as local:** run the same `IMAGE_TAG` (the short commit ID) as the local
   production stack.

### 9. Secrets
- **Vault runs on the instance**, as it does locally, with file storage on the disk. Follow the
  init and unseal guide in `/secrets/README.md`.
- **Unseal keys stay off the instance:** after every reboot, Vault must be unsealed. Keep the
  keys in the team's password manager, never on the instance or in AWS.
- **`.env` holds no secrets:** it's copied to the instance over Session Manager (step 10). Its
  secret values come from Vault, as in Section 0.7.

### 10. Install and start (on the instance)

Open a shell with no SSH:

```bash
aws ssm start-session --target "$INSTANCE_ID"
```

On the instance:

```bash
sudo apt-get update && sudo apt-get install -y docker.io docker-compose-v2 git
sudo usermod -aG docker ubuntu && newgrp docker
git clone https://github.com/jackkibort/tech-wreck-cmpe272.git securehr && cd securehr
```

**Then, from the repository root:**
1. Write `.env` with the public `SECUREHR_DOMAIN`.
2. Start the stack with project `securehr-prod`, as in the root README.
3. Unseal Vault.
4. Import the dataset (`/database/README.md`).
5. Run the seed steps: directory users, Vault secrets and sample documents.

**Reaching admin pages privately**, for example the directory admin UI, uses port forwarding:

```bash
aws ssm start-session --target "$INSTANCE_ID" \
  --document-name AWS-StartPortForwardingSession --parameters 'portNumber=17170,localPortNumber=17170'
```

**Check:**
- Sign in at `https://securehr.$DOMAIN` as `demo.employee`.
- Grafana's Security overview dashboard shows the login.

### 11. Backup

After the first successful deploy:

```bash
VOLUME_ID=$(aws ec2 describe-instances --instance-ids "$INSTANCE_ID" \
  --query 'Reservations[0].Instances[0].BlockDeviceMappings[0].Ebs.VolumeId' --output text)
aws ec2 create-snapshot --volume-id "$VOLUME_ID" --description "securehr after first deploy"
```

### 12. Optional: deploy from Jenkins

After the production promotion stage, Jenkins could run the deploy on the instance through
Session Manager with `aws ssm send-command` (pull the new tag, `docker compose up -d`). Jenkins
would then need an IAM role allowed to send commands to this one instance. This isn't built for
the proof of concept.

### 13. Day-to-day: stop when idle

```bash
aws ec2 stop-instances --instance-ids "$INSTANCE_ID"
```

```bash
aws ec2 start-instances --instance-ids "$INSTANCE_ID"
```

The Elastic IP stays attached. **After every start, unseal Vault** (step 9).

### 14. Teardown after grading

Do this in order, and check the bill a day later:

```bash
aws ec2 terminate-instances --instance-ids "$INSTANCE_ID"
aws ec2 wait instance-terminated --instance-ids "$INSTANCE_ID"
aws ec2 release-address --allocation-id "$ALLOC_ID"
aws ec2 describe-snapshots --owner-ids self --query 'Snapshots[].SnapshotId' --output text
aws ec2 delete-snapshot --snapshot-id <each id from above>
aws ec2 delete-security-group --group-id "$SG_ID"
aws iam remove-role-from-instance-profile --instance-profile-name securehr-ec2 --role-name securehr-ec2
aws iam delete-instance-profile --instance-profile-name securehr-ec2
aws iam detach-role-policy --role-name securehr-ec2 --policy-arn arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore
aws iam delete-role --role-name securehr-ec2
```

**Also clean up:**
- delete the images from GitHub Container Registry;
- remove the AWS redirect URIs from Keycloak;
- remove the production callback URL from the GitHub OAuth app.

## Security choices

| Choice | Why |
|---|---|
| Only 80 and 443 open; no SSH | Shell access through Session Manager is logged and needs IAM sign-in, with no key to leak |
| IMDSv2 required | Blocks server-side request forgery from reading the instance's credentials |
| Encrypted disk | Data at rest is protected, including in snapshots |
| One Identity Center user per teammate, no root use | Every action is attributable; root stays locked with MFA |
| Budget alerts at $10, $25, $50 | A forgotten instance is noticed within days |
| Same Compose stack, Vault and edge as locally | The layered security tested locally is exactly what runs publicly |
