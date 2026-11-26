import { render } from '@testing-library/react';
import type { ReactElement } from 'react';
import { MemoryRouter, Route, Routes } from 'react-router';
import { vi } from 'vitest';

import { SessionContext, type Session } from '../auth/session';
import { ApiClientProvider } from '../shared/api/ApiContext';
import type { Role } from '../shared/api/types';

import { fakeApi, type Routes as ApiRoutes } from './fakeApi';

/** Demo identities from database/README.md (Section 7.4). */
export const USERS = {
  employee: { username: 'demo.employee', empNo: 10012, roles: ['EMPLOYEE'] },
  manager: { username: 'demo.manager', empNo: 110567, roles: ['EMPLOYEE', 'MANAGER'] },
  hradmin: { username: 'demo.hradmin', empNo: 10005, roles: ['EMPLOYEE', 'HR_ADMIN'] },
} satisfies Record<string, { username: string; empNo: number; roles: Role[] }>;

interface Options {
  user: (typeof USERS)[keyof typeof USERS];
  api?: ApiRoutes;
  /** Initial URL, e.g. "/employees/10012". */
  url?: string;
  /** Route pattern the element is mounted at, e.g. "/employees/:empNo". */
  path?: string;
}

/** Renders a page as a signed-in user, with a fake API behind the client. */
export function renderWithSession(element: ReactElement, options: Options) {
  const api = fakeApi(options.api ?? {});
  const session: Session = {
    username: options.user.username,
    empNo: options.user.empNo,
    roles: options.user.roles,
    employee: null,
    signOut: vi.fn(),
  };
  const view = render(
    <MemoryRouter initialEntries={[options.url ?? '/']}>
      <ApiClientProvider client={api.client}>
        <SessionContext.Provider value={session}>
          <Routes>
            <Route path={options.path ?? '*'} element={element} />
          </Routes>
        </SessionContext.Provider>
      </ApiClientProvider>
    </MemoryRouter>,
  );
  return { ...view, api, session };
}
