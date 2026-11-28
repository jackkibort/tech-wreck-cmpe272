import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';

import { renderWithSession, USERS } from '../test/renderWithSession';

import { DirectoryPage } from './DirectoryPage';

const departments = [
  { deptNo: 'd005', deptName: 'Development', manager: null },
  { deptNo: 'd007', deptName: 'Sales', manager: null },
];

function page(url: URL) {
  const pageNo = Number(url.searchParams.get('page'));
  return {
    body: {
      content: [
        {
          empNo: pageNo === 0 ? 10012 : 20000,
          firstName: pageNo === 0 ? 'Patricio' : 'Second',
          lastName: pageNo === 0 ? 'Bridgland' : 'Page',
          deptNo: 'd005',
          deptName: 'Development',
          title: 'Senior Engineer',
        },
      ],
      page: pageNo,
      size: 25,
      totalElements: 26,
      totalPages: 2,
    },
  };
}

describe('Directory search (UC-02)', () => {
  it('searches with the chosen filters and links each result to its profile', async () => {
    const { api } = renderWithSession(<DirectoryPage />, {
      user: USERS.employee,
      api: { 'GET /api/departments': { body: departments }, 'GET /api/employees': page },
    });

    await userEvent.type(screen.getByLabelText('Name starts with'), 'Brid');
    await screen.findByRole('option', { name: 'Development' });
    await userEvent.selectOptions(screen.getByLabelText('Department'), 'd005');
    await userEvent.click(screen.getByRole('button', { name: 'Search' }));

    await waitFor(() => {
      expect(api.requested()).toContain('GET /api/employees?q=Brid&deptNo=d005&page=0&size=25');
    });
    expect(await screen.findByRole('link', { name: 'Bridgland, Patricio' })).toHaveAttribute(
      'href',
      '/employees/10012',
    );
  });

  it('pages through results', async () => {
    const { api } = renderWithSession(<DirectoryPage />, {
      user: USERS.employee,
      api: { 'GET /api/departments': { body: departments }, 'GET /api/employees': page },
    });

    await userEvent.click(await screen.findByRole('button', { name: 'Next' }));

    expect(await screen.findByRole('link', { name: 'Page, Second' })).toBeInTheDocument();
    expect(api.requested()).toContain('GET /api/employees?page=1&size=25');
    expect(screen.getByText(/Page 2 of 2/)).toBeInTheDocument();
  });

  it('says so when nothing matches', async () => {
    renderWithSession(<DirectoryPage />, {
      user: USERS.employee,
      api: {
        'GET /api/departments': { body: departments },
        'GET /api/employees': {
          body: { content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 },
        },
      },
    });

    expect(await screen.findByText('No employees match these filters.')).toBeInTheDocument();
  });
});
