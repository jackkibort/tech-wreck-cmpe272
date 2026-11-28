/**
 * Where the tests run and who they sign in as. Everything comes from environment variables so
 * the same suite runs against staging (Jenkins stage 12) and production.
 *
 * Passwords and one-time-password seeds are never in this repository. In Jenkins they come from
 * Vault (see /e2e/README.md); by hand, put them in your shell for the run only.
 */

const domain = process.env.SECUREHR_DOMAIN ?? 'securehr.local';
const staging = (process.env.E2E_TARGET ?? 'staging') === 'staging';

/** Public URLs of the portal and the tools (Section 0.3). */
export const urls = {
  portal: process.env.PORTAL_URL ?? (staging ? `https://staging.${domain}:8443` : `https://${domain}`),
  grafana: process.env.GRAFANA_URL ?? `https://grafana.${domain}`,
  jenkins: process.env.JENKINS_URL ?? `https://ci.${domain}`,
};

/** Demo identities from Section 7.4 and the emp_no picks in /database/README.md. */
export const demo = {
  employee: { username: 'demo.employee', empNo: 10012, lastName: 'Bridgland' },
  manager: { username: 'demo.manager', empNo: 110567 },
  employee2: { username: 'demo.employee2', empNo: 10041 },
  hradmin: { username: 'demo.hradmin', empNo: 10005 },
  auditor: { username: 'demo.auditor' },
  dev: { username: process.env.E2E_DEV_USERNAME ?? '' },
} as const;

/** Name of the environment variable holding a user's password, e.g. E2E_PASSWORD_DEMO_HRADMIN. */
function envName(prefix: string, username: string): string {
  return `${prefix}_${username.replace(/[^A-Za-z0-9]/g, '_').toUpperCase()}`;
}

/** The user's directory password. Fails the test with a clear message when it isn't set. */
export function passwordFor(username: string): string {
  const name = envName('E2E_PASSWORD', username);
  const value = process.env[name];
  if (!value) throw new Error(`Set ${name} (the password for ${username}, from Vault)`);
  return value;
}

/** The user's one-time-password seed (base32), for MFA users (HR_ADMIN, AUDITOR). */
export function totpSecretFor(username: string): string {
  const name = envName('E2E_TOTP', username);
  const value = process.env[name];
  if (!value) throw new Error(`Set ${name} (the OTP seed for ${username}, from Vault)`);
  return value;
}
