import { expect, test } from '@playwright/test';
import { demo, urls } from '../support/env';
import { signIn, signInToPortal } from '../support/login';
import { AUDIT_FIELDS, auditEvents } from '../support/grafana';

// Audit trail (goal G4; use case UC-06; scenario S7). Runs last, so earlier tests' events exist.

test('TC-AUD-01 N salary views produce exactly N audit events with all fields', async ({ browser }) => {
  const start = new Date();
  const views = 3;

  // HR Admin opens the same employee's salary history N times; count the API calls the page made.
  const hrContext = await browser.newContext();
  const hr = await hrContext.newPage();
  let salaryReads = 0;
  hr.on('response', (r) => {
    if (r.url().includes(`/api/employees/${demo.employee.empNo}/salaries`) && r.status() === 200) salaryReads += 1;
  });
  await signInToPortal(hr, demo.hradmin.username, { mfa: true });
  for (let i = 0; i < views; i += 1) {
    await hr.goto(`/employees/${demo.employee.empNo}`);
    await expect(hr.getByRole('heading', { name: 'Salary history' })).toBeVisible();
  }
  await hrContext.close();
  expect(salaryReads).toBeGreaterThanOrEqual(views);

  // The auditor reads the trail through Grafana; events arrive within seconds (G4).
  const auditorContext = await browser.newContext({ ignoreHTTPSErrors: true });
  const auditor = await auditorContext.newPage();
  await auditor.goto(urls.grafana);
  await signIn(auditor, demo.auditor.username, { mfa: true });
  await expect(auditor).toHaveURL(new RegExp('^' + new URL(urls.grafana).origin));

  const query =
    '{log_type="audit"} | json | event_type="SALARY_READ"' +
    ` | actor_username="${demo.hradmin.username}" | target_emp_no="${demo.employee.empNo}"`;
  await expect
    .poll(async () => (await auditEvents(auditor.request, query, start, new Date())).length, { timeout: 30_000 })
    .toBe(salaryReads);

  for (const event of await auditEvents(auditor.request, query, start, new Date())) {
    for (const field of AUDIT_FIELDS) expect(event, `audit field ${field}`).toHaveProperty(field);
    expect(event['outcome']).toBe('ALLOWED');
  }
  await auditorContext.close();
});

test('TC-E2E-05 auditor sees the audit dashboards with the events just generated', async ({ page }) => {
  await page.goto(urls.grafana);
  await signIn(page, demo.auditor.username, { mfa: true });

  await page.goto(`${urls.grafana}/d/securehr-sensitive`);
  await expect(page.getByText('Latest sensitive-data events')).toBeVisible();
  await expect(page.getByText('SALARY_READ').first()).toBeVisible({ timeout: 30_000 });

  await page.goto(`${urls.grafana}/d/securehr-security`);
  await expect(page.getByText('Latest security events')).toBeVisible();

  // Auditors see the Audit folder only (Section 0.5): Grafana refuses the platform dashboard.
  const platform = await page.request.get(`${urls.grafana}/api/dashboards/uid/securehr-platform`);
  expect([403, 404]).toContain(platform.status());
});
