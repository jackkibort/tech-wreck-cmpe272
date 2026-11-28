import { describe, expect, it, vi } from 'vitest';

import { buildOidcSettings, loadPortalConfig } from './oidcConfig';

const config = {
  authority: 'https://auth.securehr.local/realms/securehr',
  clientId: 'securehr-portal',
};

describe('OIDC settings (Section 0.5, 4.3)', () => {
  const settings = buildOidcSettings(config, 'https://securehr.local');

  it('uses the authorization code flow for the securehr-portal client', () => {
    expect(settings.authority).toBe(config.authority);
    expect(settings.client_id).toBe('securehr-portal');
    expect(settings.response_type).toBe('code');
    expect(settings.redirect_uri).toBe('https://securehr.local/');
    expect(settings.post_logout_redirect_uri).toBe('https://securehr.local/');
  });

  it('keeps tokens in memory, never in localStorage or sessionStorage', async () => {
    localStorage.clear();
    sessionStorage.clear();

    await settings.userStore?.set('user:securehr-portal', '{"access_token":"secret"}');

    expect(await settings.userStore?.get('user:securehr-portal')).toContain('secret');
    expect(localStorage.length).toBe(0);
    expect(sessionStorage.length).toBe(0);
  });

  it('renews with refresh tokens and opens no session-monitor iframe', () => {
    expect(settings.automaticSilentRenew).toBe(true);
    expect(settings.monitorSession).toBe(false);
  });
});

describe('loadPortalConfig', () => {
  it('reads /config.json without caching', async () => {
    const fetchFn = vi.fn(() => Promise.resolve(new Response(JSON.stringify(config))));

    await expect(loadPortalConfig(fetchFn as unknown as typeof fetch)).resolves.toEqual(config);
    expect(fetchFn).toHaveBeenCalledWith('/config.json', { cache: 'no-store' });
  });

  it('fails clearly when the file is missing', async () => {
    const fetchFn = () => Promise.resolve(new Response('', { status: 404 }));

    await expect(loadPortalConfig(fetchFn as typeof fetch)).rejects.toThrow(
      /configuration \(404\)/,
    );
  });
});
