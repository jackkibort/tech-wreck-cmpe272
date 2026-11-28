import { expect, test } from '@playwright/test';
import { demo } from '../support/env';
import { signInToPortal } from '../support/login';

// Directory, profile and manager scope (use cases UC-02 to UC-04; scenarios S1 and S3).

test('TC-E2E-01 employee searches the directory and opens own profile with salary chart', async ({ page }) => {
  await signInToPortal(page, demo.employee.username);
  await page.getByLabel('Name starts with').fill(demo.employee.lastName);
  await page.getByRole('button', { name: 'Search' }).click();
  await page.getByRole('link', { name: new RegExp(`^${demo.employee.lastName},`) }).first().click();

  await expect(page).toHaveURL(new RegExp(`/employees/${demo.employee.empNo}$`));
  await expect(page.getByRole('heading', { name: 'Title history' })).toBeVisible();
  // Own record: salary history is shown (Section 8.2).
  await expect(page.getByRole('heading', { name: 'Salary history' })).toBeVisible();
  await expect(page.getByText('Current salary')).toBeVisible();
});

test('TC-E2E-02 manager views a team member with no salary shown', async ({ page }) => {
  await signInToPortal(page, demo.manager.username);
  await page.goto(`/employees/${demo.employee.empNo}`);

  await expect(page.getByRole('heading', { name: 'Title history' })).toBeVisible();
  // Masked in the API, so the page has nothing to show (Section 8.2).
  await expect(page.getByRole('heading', { name: 'Salary history' })).toHaveCount(0);
  await expect(page.getByText('Current salary')).toHaveCount(0);
  await expect(page.getByText('Birth date')).toHaveCount(0);
});
