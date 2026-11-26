import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import { describe, expect, it, vi } from 'vitest';

import { AppRoutes } from './AppRoutes';
import { SessionContext, type Session } from './auth/session';
import { ApiClientProvider } from './shared/api/ApiContext';
import { fakeApi } from './test/fakeApi';

function renderAt(url: string) {
  const { client } = fakeApi({
    'GET /api/departments': { body: [] },
    'GET /api/employees': {
      body: { content: [], page: 0, size: 25, totalElements: 0, totalPages: 0 },
    },
  });
  const session: Session = {
    username: 'demo.employee',
    empNo: 10012,
    roles: ['EMPLOYEE'],
    employee: null,
    signOut: vi.fn(),
  };
  render(
    <MemoryRouter initialEntries={[url]}>
      <ApiClientProvider client={client}>
        <SessionContext.Provider value={session}>
          <AppRoutes />
        </SessionContext.Provider>
      </ApiClientProvider>
    </MemoryRouter>,
  );
}

describe('AppRoutes', () => {
  it('sends a portal user from / to the directory', async () => {
    renderAt('/');

    expect(await screen.findByRole('heading', { name: 'Employee directory' })).toBeInTheDocument();
  });

  it('shows a not-found message for unknown paths', () => {
    renderAt('/nope');

    expect(screen.getByRole('alert')).toHaveTextContent('Page not found.');
  });
});
