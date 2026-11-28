import { readFile } from 'node:fs/promises';
import { expect, test } from '@playwright/test';
import { demo } from '../support/env';
import { signInToPortal } from '../support/login';

// HR operations: upload and export (use cases UC-05, UC-07, UC-08; scenario S4).

/** A minimal, valid one-page PDF, so the Document API's content check (Section 10.1) accepts it. */
function samplePdf(title: string): Buffer {
  const text = `%PDF-1.4
1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj
2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj
3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 300 144] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj
4 0 obj << /Length 44 >> stream
BT /F1 12 Tf 20 100 Td (${title}) Tj ET
endstream endobj
5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj
trailer << /Root 1 0 R >>
%%EOF
`;
  return Buffer.from(text, 'latin1');
}

test('TC-E2E-03 HR Admin uploads a payslip; that employee can download it', async ({ browser }) => {
  const title = `E2E payslip ${Date.now()}`;

  const hrContext = await browser.newContext();
  const hr = await hrContext.newPage();
  await signInToPortal(hr, demo.hradmin.username, { mfa: true });
  await hr.getByRole('link', { name: 'Documents' }).click();
  await hr.getByLabel('PDF file (max 10 MB)').setInputFiles({ name: 'payslip.pdf', mimeType: 'application/pdf', buffer: samplePdf(title) });
  await hr.getByLabel('Category').selectOption({ label: 'Payslip' });
  await hr.getByLabel('Title').fill(title);
  await hr.getByLabel('Employee number').fill(String(demo.employee.empNo));
  await hr.getByRole('button', { name: 'Upload' }).click();
  await expect(hr.getByRole('cell', { name: title })).toBeVisible();
  await hr.screenshot({ path: test.info().outputPath('hr-after-upload.png') });
  await hrContext.close();

  const employeeContext = await browser.newContext();
  const employee = await employeeContext.newPage();
  await signInToPortal(employee, demo.employee.username);
  await employee.getByRole('link', { name: 'Documents' }).click();
  const row = employee.getByRole('row', { name: new RegExp(title) });
  const [download] = await Promise.all([
    employee.waitForEvent('download'),
    row.getByRole('button', { name: /^Download/ }).click(),
  ]);
  const content = await readFile(await download.path());
  expect(content.subarray(0, 5).toString('latin1')).toBe('%PDF-');
  await employee.screenshot({ path: test.info().outputPath('employee-after-download.png') });
  await employeeContext.close();
});

test('TC-E2E-04 HR Admin exports a filtered CSV', async ({ page }) => {
  await signInToPortal(page, demo.hradmin.username, { mfa: true });
  await page.getByRole('link', { name: 'Export' }).click();
  await page.getByLabel('Department').selectOption('d005');
  const [download] = await Promise.all([
    page.waitForEvent('download'),
    page.getByRole('button', { name: 'Export CSV' }).click(),
  ]);
  expect(download.suggestedFilename()).toBe('employees-export.csv');
  const lines = (await readFile(await download.path(), 'utf8')).trim().split(/\r?\n/);
  expect(lines[0]).toContain('emp_no');
  // Header plus at most 10,000 rows (Section 0.6 export cap).
  expect(lines.length).toBeGreaterThan(1);
  expect(lines.length).toBeLessThanOrEqual(10_001);
  await expect(page.getByText('Downloaded employees-export.csv')).toBeVisible();
});
