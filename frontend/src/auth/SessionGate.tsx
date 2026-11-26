import { useEffect, type ReactNode } from 'react';
import { useAuth } from 'react-oidc-context';

import { ApiClientProvider } from '../shared/api/ApiContext';
import { ApiError, type ApiClient } from '../shared/api/client';
import type { Me } from '../shared/api/types';
import { ErrorMessage } from '../shared/components/ErrorMessage';
import { Loading } from '../shared/components/Loading';
import { useApiResource } from '../shared/hooks/useApiResource';

import { NoPortalAccess } from './NoPortalAccess';
import { SessionContext, type Session } from './session';

/**
 * Signs the user in (redirect to Keycloak, Section 5.1), then loads GET /api/me and provides the
 * session and the API client to the rest of the app. The client is created once at startup
 * (main.tsx) and reads the current token from the OIDC UserManager on every request.
 */
export function SessionGate({ client, children }: { client: ApiClient; children: ReactNode }) {
  const auth = useAuth();
  const needsSignIn =
    !auth.isLoading && !auth.isAuthenticated && !auth.activeNavigator && !auth.error;

  // Once the redirect starts, activeNavigator is set and needsSignIn turns false, so this runs once.
  useEffect(() => {
    if (needsSignIn) {
      void auth.signinRedirect();
    }
  }, [needsSignIn, auth]);

  if (auth.error) {
    return (
      <main className="centered">
        <ErrorMessage
          error={new ApiError(0, `Sign-in failed: ${auth.error.message}`, null)}
          onRetry={() => void auth.signinRedirect()}
        />
      </main>
    );
  }
  if (auth.isLoading || auth.activeNavigator) {
    return <Loading label="Signing you in…" />;
  }
  if (!auth.isAuthenticated) {
    return <Loading label="Redirecting to sign-in…" />;
  }

  return (
    <ApiClientProvider client={client}>
      <MeLoader signOut={() => void auth.signoutRedirect()}>{children}</MeLoader>
    </ApiClientProvider>
  );
}

function MeLoader({ signOut, children }: { signOut: () => void; children: ReactNode }) {
  const me = useApiResource<Me>('me', (client, signal) =>
    client.getJson<Me>('/api/me', undefined, signal),
  );

  if (me.loading) {
    return <Loading label="Loading your profile…" />;
  }
  if (me.error instanceof ApiError && me.error.status === 403) {
    return <NoPortalAccess signOut={signOut} />;
  }
  if (me.error !== undefined || !me.data) {
    return (
      <main className="centered">
        <ErrorMessage error={me.error} onRetry={me.reload} />
      </main>
    );
  }

  const session: Session = {
    username: me.data.username,
    empNo: me.data.empNo ?? null,
    roles: me.data.roles,
    employee: me.data.employee ?? null,
    signOut,
  };
  return <SessionContext.Provider value={session}>{children}</SessionContext.Provider>;
}
