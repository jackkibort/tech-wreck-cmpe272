import { InMemoryWebStorage, WebStorageStateStore, type UserManagerSettings } from 'oidc-client-ts';

/** Runtime settings served as /config.json by the ui container (see docker/). */
export interface PortalConfig {
  authority: string;
  clientId: string;
}

/**
 * OIDC settings for the securehr-portal public client (Section 0.5).
 *
 * - Authorization code flow with PKCE (oidc-client-ts always uses S256).
 * - Tokens are kept in memory only (Section 4.3): a reload signs in again silently through the
 *   Keycloak SSO session. Only the short-lived PKCE state for the redirect itself is kept in
 *   sessionStorage, because it must survive the round trip to Keycloak; it holds no token and is
 *   removed when the callback completes.
 * - Tokens are renewed with the refresh token, so no hidden iframe is needed and the portal's CSP
 *   can forbid all frames. Session-monitor iframes are off for the same reason.
 */
export function buildOidcSettings(config: PortalConfig, origin: string): UserManagerSettings {
  return {
    authority: config.authority,
    client_id: config.clientId,
    redirect_uri: `${origin}/`,
    post_logout_redirect_uri: `${origin}/`,
    response_type: 'code',
    scope: 'openid profile',
    userStore: new WebStorageStateStore({ store: new InMemoryWebStorage() }),
    automaticSilentRenew: true,
    monitorSession: false,
  };
}

/** Loads /config.json; the same image serves production and staging (Section 11.3). */
export async function loadPortalConfig(
  fetchFn: typeof fetch = window.fetch.bind(window),
): Promise<PortalConfig> {
  const response = await fetchFn('/config.json', { cache: 'no-store' });
  if (!response.ok) {
    throw new Error(`Could not load portal configuration (${response.status}).`);
  }
  return (await response.json()) as PortalConfig;
}
