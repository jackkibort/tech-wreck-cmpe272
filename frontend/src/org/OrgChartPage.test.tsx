import { screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';

import { renderWithSession, USERS } from '../test/renderWithSession';

import { OrgChartPage } from './OrgChartPage';

describe('Org chart', () => {
  it('shows the department manager above the employee', async () => {
    renderWithSession(<OrgChartPage />, {
      user: USERS.employee,
      url: '/org/10012',
      path: '/org/:empNo',
      api: {
        'GET /api/org/10012': {
          body: {
            deptNo: 'd005',
            deptName: 'Development',
            chain: [
              {
                empNo: 10012,
                firstName: 'Patricio',
                lastName: 'Bridgland',
                title: 'Senior Engineer',
              },
              { empNo: 110567, firstName: 'Leon', lastName: 'DasSarma', title: 'Manager' },
            ],
          },
        },
      },
    });

    const items = await screen.findAllByRole('listitem');
    expect(items[0]).toHaveTextContent('Leon DasSarma');
    expect(items[0]).toHaveTextContent('Department manager');
    expect(items[1]).toHaveTextContent('Patricio Bridgland');
    expect(screen.getByText('Department: Development')).toBeInTheDocument();
  });
});
