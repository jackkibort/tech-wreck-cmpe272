import { screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { errorBody } from '../test/fakeApi';
import { renderWithSession, USERS } from '../test/renderWithSession';

import { EmployeePage } from './EmployeePage';

const patricio = {
  empNo: 10012,
  firstName: 'Patricio',
  lastName: 'Bridgland',
  gender: 'M',
  hireDate: '1992-12-18',
  deptNo: 'd005',
  deptName: 'Development',
  title: 'Senior Engineer',
};
const titles = [{ title: 'Senior Engineer', fromDate: '2000-12-18', toDate: null }];
const salaries = [
  { salary: 54423, fromDate: '1992-12-18', toDate: '1993-12-18' },
  { salary: 60117, fromDate: '1993-12-18', toDate: null },
];

function render(
  user: (typeof USERS)[keyof typeof USERS],
  api: Parameters<typeof renderWithSession>[1]['api'],
  empNo = 10012,
) {
  return renderWithSession(<EmployeePage />, {
    user,
    api,
    url: `/employees/${empNo}`,
    path: '/employees/:empNo',
  });
}

describe('TC-UI-01 employee page hides salary from roles without access', () => {
  it('employee viewing their own record sees birth date, salary and the salary history', async () => {
    render(USERS.employee, {
      'GET /api/employees/10012': {
        body: { ...patricio, birthDate: '1964-12-18', currentSalary: 60117 },
      },
      'GET /api/employees/10012/titles': { body: titles },
      'GET /api/employees/10012/salaries': { body: salaries },
    });

    expect(await screen.findByRole('heading', { name: /Patricio Bridgland/ })).toBeInTheDocument();
    expect(screen.getByText('1964-12-18')).toBeInTheDocument();
    const table = await screen.findByRole('table', { name: 'Salary history' });
    expect(within(table).getAllByText('$60,117')).toHaveLength(1);
    expect(within(table).getByText('Present')).toBeInTheDocument();
  });

  it('manager viewing a team member sees no salary section and never requests salaries', async () => {
    const { api } = render(USERS.manager, {
      // The API masks the fields; the page must not add a salary section on its own.
      'GET /api/employees/10012': { body: patricio },
      'GET /api/employees/10012/titles': { body: titles },
    });

    expect(await screen.findByRole('heading', { name: /Patricio Bridgland/ })).toBeInTheDocument();
    expect(await screen.findByRole('table', { name: 'Title history' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Salary history' })).not.toBeInTheDocument();
    expect(screen.queryByText('Current salary')).not.toBeInTheDocument();
    expect(screen.queryByText('Birth date')).not.toBeInTheDocument();
    expect(api.requested()).not.toContain('GET /api/employees/10012/salaries');
  });

  it('HR Admin viewing another employee sees the salary history', async () => {
    render(USERS.hradmin, {
      'GET /api/employees/10012': { body: { ...patricio, currentSalary: 60117 } },
      'GET /api/employees/10012/titles': { body: titles },
      'GET /api/employees/10012/salaries': { body: salaries },
    });

    expect(await screen.findByRole('table', { name: 'Salary history' })).toBeInTheDocument();
    expect(screen.getByText('Current salary')).toBeInTheDocument();
  });
});

describe('TC-UI-02 employee page handles 401, 403 and 429', () => {
  it('403: shows the access-denied message from the API', async () => {
    render(
      USERS.employee,
      { 'GET /api/employees/10041': errorBody(403, 'You may not view this employee.') },
      10041,
    );

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Access denied');
    expect(alert).toHaveTextContent('You may not view this employee.');
    expect(alert).toHaveTextContent('Request ID: req-403');
    expect(within(alert).queryByRole('button')).not.toBeInTheDocument();
  });

  it('401: starts a new sign-in and says so', async () => {
    const { api } = render(USERS.employee, {
      'GET /api/employees/10012': errorBody(401, 'Expired'),
    });

    expect(await screen.findByRole('alert')).toHaveTextContent('Your session has expired');
    expect(api.onUnauthorized).toHaveBeenCalledTimes(1);
  });

  it('429: asks the user to wait, and "Try again" repeats the request', async () => {
    let calls = 0;
    render(USERS.employee, {
      'GET /api/employees/10012': () => {
        calls += 1;
        return calls === 1 ? errorBody(429, 'slow down') : { body: patricio };
      },
      'GET /api/employees/10012/titles': { body: titles },
      'GET /api/employees/10012/salaries': { body: salaries },
    });

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('Too many requests');
    await userEvent.click(within(alert).getByRole('button', { name: 'Try again' }));

    expect(await screen.findByRole('heading', { name: /Patricio Bridgland/ })).toBeInTheDocument();
    await waitFor(() => {
      expect(calls).toBe(2);
    });
  });

  it('rejects an invalid employee number without calling the API', () => {
    const { api } = render(USERS.employee, {}, 0);

    expect(screen.getByRole('alert')).toHaveTextContent('not a valid employee number');
    expect(api.fetchFn).not.toHaveBeenCalled();
  });
});
