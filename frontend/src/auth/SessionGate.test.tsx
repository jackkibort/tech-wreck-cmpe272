import { render, screen } from '@testing-library/react';
import type { AuthContextProps } from 'react-oidc-context';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { errorBody, fakeApi } from '../test/fakeApi';

import { useSession } from './session';
import { SessionGate } from './SessionGate';

const auth = {
  isLoading: false,
  isAuthenticated: true,
  activeNavigator: undefined,
  error: undefined,
  signinRedirect: vi.fn(() => Promise.resolve()),
  signoutRedirect: vi.fn(() => Promise.resolve()),
} as unknown as AuthContextProps;

vi.mock('react-oidc-context', () => ({ useAuth: () => auth }));

function WhoAmI() {
  const session = useSession();
  return (
    <p>
      {session.username} {session.roles.join(',')} {session.empNo}
    </p>
  );
}

beforeEach(() => {
  vi.clearAllMocks();
  Object.assign(auth, {
    isLoading: false,
    isAuthenticated: true,
    activeNavigator: undefined,
    error: undefined,
  });
});

describe('SessionGate', () => {
  it('redirects to Keycloak when the user is not signed in (UC-01)', () => {
    Object.assign(auth, { isAuthenticated: false });
    const { client, fetchFn } = fakeApi({});

    render(
      <SessionGate client={client}>
        <WhoAmI />
      </SessionGate>,
    );

    expect(screen.getByRole('status')).toHaveTextContent('Redirecting to sign-in');
    expect(auth.signinRedirect).toHaveBeenCalledTimes(1);
    expect(fetchFn).not.toHaveBeenCalled();
  });

  it('provides the session from GET /api/me', async () => {
    const { client } = fakeApi({
      'GET /api/me': {
        body: { username: 'demo.manager', empNo: 110567, roles: ['EMPLOYEE', 'MANAGER'] },
      },
    });

    render(
      <SessionGate client={client}>
        <WhoAmI />
      </SessionGate>,
    );

    expect(await screen.findByText('demo.manager EMPLOYEE,MANAGER 110567')).toBeInTheDocument();
  });

  it('shows the no-access page when /api/me answers 403 (Auditor, unlinked GitHub account)', async () => {
    const { client } = fakeApi({ 'GET /api/me': errorBody(403, 'Forbidden') });

    render(
      <SessionGate client={client}>
        <WhoAmI />
      </SessionGate>,
    );

    expect(
      await screen.findByRole('heading', { name: 'No access to the employee portal' }),
    ).toBeInTheDocument();
    screen.getByRole('button', { name: 'Sign out' }).click();
    expect(auth.signoutRedirect).toHaveBeenCalledTimes(1);
  });

  it('shows sign-in errors with a retry', () => {
    Object.assign(auth, { error: new Error('invalid_client') });
    const { client } = fakeApi({});

    render(
      <SessionGate client={client}>
        <WhoAmI />
      </SessionGate>,
    );

    expect(screen.getByRole('alert')).toHaveTextContent('Sign-in failed: invalid_client');
  });
});
