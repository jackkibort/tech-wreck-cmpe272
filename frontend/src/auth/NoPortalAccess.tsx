/**
 * Shown when GET /api/me answers 403: the user signed in but holds no portal role (for example
 * an Auditor, who reviews events in Grafana instead, UC-06, or an unlinked GitHub account).
 */
export function NoPortalAccess({ signOut }: { signOut: () => void }) {
  return (
    <main className="centered">
      <section className="card" aria-labelledby="no-access-title">
        <h1 id="no-access-title">No access to the employee portal</h1>
        <p>
          Your account is signed in but has no employee portal role. Auditors review security and
          access events in Grafana.
        </p>
        <p>If you signed in with GitHub, link the account to your directory account first.</p>
        <button type="button" className="button" onClick={signOut}>
          Sign out
        </button>
      </section>
    </main>
  );
}
