import { expect, test } from '@playwright/test';

// TC-SEC-02: the edge sets the security headers on every response (Section 8.4).

const REQUIRED = {
  'strict-transport-security': /max-age=\d+/,
  'content-security-policy': /default-src/,
  'x-content-type-options': /^nosniff$/i,
  'x-frame-options': /^deny$/i,
  'referrer-policy': /.+/,
  'permissions-policy': /.+/,
} as const;

for (const path of ['/', '/api/me', '/api/documents']) {
  test(`TC-SEC-02 security headers on ${path}`, async ({ request }) => {
    // No token: the APIs answer 401, which must carry the headers too.
    const response = await request.get(path, { maxRedirects: 0 });
    const headers = response.headers();
    for (const [name, pattern] of Object.entries(REQUIRED)) {
      expect(headers[name], `${name} on ${path}`).toMatch(pattern);
    }
    // The NGINX version is hidden (Section 8.4).
    expect(headers['server'] ?? '').not.toMatch(/\d/);
  });
}
