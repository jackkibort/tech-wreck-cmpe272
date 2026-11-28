import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';

import { errorBody } from '../test/fakeApi';
import { renderWithSession, USERS } from '../test/renderWithSession';

import { ExportPage } from './ExportPage';

describe('TC-UI-01 export page', () => {
  it('shows no export form to an employee', () => {
    const { api } = renderWithSession(<ExportPage />, { user: USERS.employee });

    expect(screen.getByRole('alert')).toHaveTextContent('HR Admins only');
    expect(screen.queryByRole('button', { name: 'Export CSV' })).not.toBeInTheDocument();
    expect(api.fetchFn).not.toHaveBeenCalled();
  });
});

describe('HR Admin CSV export (UC-05)', () => {
  beforeEach(() => {
    URL.createObjectURL = vi.fn(() => 'blob:export');
    URL.revokeObjectURL = vi.fn();
    vi.spyOn(HTMLAnchorElement.prototype, 'click').mockImplementation(() => undefined);
  });
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('downloads the CSV for the chosen filters and reports truncation', async () => {
    const { api } = renderWithSession(<ExportPage />, {
      user: USERS.hradmin,
      api: {
        'GET /api/departments': { body: [{ deptNo: 'd005', deptName: 'Development' }] },
        'GET /api/export/employees': {
          body: 'emp_no,first_name\n10012,Patricio',
          headers: {
            'Content-Type': 'text/csv',
            'Content-Disposition': 'attachment; filename="employees-export.csv"',
            'X-Export-Truncated': 'true',
          },
        },
      },
    });

    await screen.findByRole('option', { name: 'Development' });
    await userEvent.selectOptions(screen.getByLabelText('Department'), 'd005');
    await userEvent.click(screen.getByRole('button', { name: 'Export CSV' }));

    expect(await screen.findByText(/Downloaded employees-export.csv/)).toHaveTextContent(
      '10,000 rows',
    );
    expect(api.requested()).toContain('GET /api/export/employees?deptNo=d005');
    expect(HTMLAnchorElement.prototype.click).toHaveBeenCalledTimes(1);
  });

  it('TC-UI-02: shows the 429 retry message when the export is rate-limited', async () => {
    renderWithSession(<ExportPage />, {
      user: USERS.hradmin,
      api: {
        'GET /api/departments': { body: [] },
        'GET /api/export/employees': errorBody(429, 'Too many requests'),
      },
    });

    await userEvent.click(screen.getByRole('button', { name: 'Export CSV' }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent('Too many requests');
    });
  });
});
