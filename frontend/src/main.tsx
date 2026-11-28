import { UserManager } from 'oidc-client-ts';
import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { AuthProvider } from 'react-oidc-context';
import { BrowserRouter } from 'react-router';

import { AppRoutes } from './AppRoutes';
import { buildOidcSettings, loadPortalConfig } from './auth/oidcConfig';
import { SessionGate } from './auth/SessionGate';
import { ApiClient } from './shared/api/client';
import './styles.css';

const root = createRoot(document.getElementById('root') as HTMLElement);

/** Removes ?code=&state= from the address bar after Keycloak redirects back. */
function onSigninCallback(): void {
  window.history.replaceState({}, document.title, window.location.pathname);
}

loadPortalConfig().then(
  (config) => {
    const userManager = new UserManager(buildOidcSettings(config, window.location.origin));
    const client = new ApiClient({
      getAccessToken: async () => (await userManager.getUser())?.access_token,
      // 401: the token expired or was revoked; sign in again (TC-UI-02).
      onUnauthorized: () => void userManager.signinRedirect(),
    });

    root.render(
      <StrictMode>
        <AuthProvider userManager={userManager} onSigninCallback={onSigninCallback}>
          <BrowserRouter>
            <SessionGate client={client}>
              <AppRoutes />
            </SessionGate>
          </BrowserRouter>
        </AuthProvider>
      </StrictMode>,
    );
  },
  (error: unknown) => {
    root.render(
      <main className="centered">
        <p role="alert">{error instanceof Error ? error.message : 'Could not start the portal.'}</p>
      </main>,
    );
  },
);
