// Gets an access token for the load tests (Section 13.1). Two ways, in order:
//   1. ACCESS_TOKEN: a token passed in with -e, for example one minted by a stand-in issuer.
//   2. A real sign-in through Keycloak as the portal does it: authorization code + PKCE (S256)
//      for client securehr-portal, posting the directory username and password to Keycloak's
//      login form. Use an account without MFA (demo.employee).
// Runs once in setup(), so the login itself is never part of the measured requests.
import http from 'k6/http';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';

/** Random URL-safe string for the PKCE verifier and the OAuth state. */
function randomString(bytes) {
  return encoding.b64encode(crypto.randomBytes(bytes), 'rawurl');
}

/**
 * Returns an access token, either from ACCESS_TOKEN or by signing in through Keycloak.
 * @param {string} portalUrl e.g. https://securehr.local
 * @param {string} issuer e.g. https://auth.securehr.local/realms/securehr
 */
export function accessToken(portalUrl, issuer) {
  if (__ENV.ACCESS_TOKEN) {
    return __ENV.ACCESS_TOKEN;
  }
  const username = __ENV.SECUREHR_USERNAME || 'demo.employee';
  const password = __ENV.SECUREHR_PASSWORD;
  if (!password) {
    throw new Error('Set ACCESS_TOKEN, or SECUREHR_PASSWORD for ' + username + ' (from Vault)');
  }

  const verifier = randomString(32);
  const challenge = crypto.sha256(verifier, 'base64rawurl');
  const redirectUri = portalUrl + '/';
  const authUrl =
    issuer + '/protocol/openid-connect/auth' +
    '?client_id=securehr-portal&response_type=code&scope=openid' +
    '&redirect_uri=' + encodeURIComponent(redirectUri) +
    '&code_challenge_method=S256&code_challenge=' + challenge +
    '&state=' + randomString(16);

  // 1. Keycloak's login page; the form's action URL carries the session for this login.
  const page = http.get(authUrl, { tags: { name: 'login' } });
  const match = /<form[^>]*id="kc-form-login"[^>]*action="([^"]+)"/.exec(page.body);
  if (!match) {
    throw new Error('Keycloak login form not found (status ' + page.status + ')');
  }
  const action = match[1].replace(/&amp;/g, '&');

  // 2. Post the credentials. Keycloak answers with a redirect to the portal carrying the code.
  const login = http.post(action, { username, password, credentialId: '' }, {
    redirects: 0,
    tags: { name: 'login' },
  });
  const location = login.headers.Location || '';
  const code = /[?&]code=([^&]+)/.exec(location);
  if (!code) {
    throw new Error('Login did not return a code (status ' + login.status + '); wrong password or MFA required?');
  }

  // 3. Exchange the code for tokens, proving possession of the PKCE verifier.
  const token = http.post(issuer + '/protocol/openid-connect/token', {
    grant_type: 'authorization_code',
    client_id: 'securehr-portal',
    code: decodeURIComponent(code[1]),
    redirect_uri: redirectUri,
    code_verifier: verifier,
  }, { tags: { name: 'login' } });
  if (token.status !== 200) {
    throw new Error('Token exchange failed with status ' + token.status);
  }
  return token.json('access_token');
}
