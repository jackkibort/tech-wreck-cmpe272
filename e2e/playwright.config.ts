import { defineConfig, devices } from '@playwright/test';
import { urls } from './support/env';

/**
 * End-to-end tests (Section 13.2), run by Jenkins stage 12 against staging and by hand against
 * production. Every test saves a screenshot at the end (Section 13.5), kept in the HTML report.
 */
export default defineConfig({
  testDir: './tests',
  // Tests share demo accounts (Section 7.4), so they run one at a time to stay independent.
  workers: 1,
  fullyParallel: false,
  forbidOnly: true,
  retries: 0,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: [
    ['list'],
    ['html', { outputFolder: 'reports/html', open: 'never' }],
    ['junit', { outputFile: 'reports/junit.xml' }],
  ],
  outputDir: 'test-results',
  use: {
    baseURL: urls.portal,
    screenshot: 'on',
    trace: 'retain-on-failure',
    // The local certificates come from mkcert, which Chromium in the test container doesn't
    // trust. TLS itself is checked at the edge (Section 15.3), not by these journeys.
    ignoreHTTPSErrors: true,
    actionTimeout: 10_000,
    navigationTimeout: 20_000,
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
});
