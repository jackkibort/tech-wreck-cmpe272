import { NavLink, Outlet } from 'react-router';

import { hasPortalRole, isHrAdmin } from '../../auth/roles';
import { useSession } from '../../auth/session';

/** Page frame: skip link, header with role-aware navigation, and the routed page. */
export function Layout() {
  const session = useSession();
  const portal = hasPortalRole(session);
  const linkClass = ({ isActive }: { isActive: boolean }) =>
    isActive ? 'nav-link active' : 'nav-link';

  return (
    <>
      <a className="skip-link" href="#main">
        Skip to content
      </a>
      <header className="app-header">
        <span className="brand">SecureHR</span>
        <nav aria-label="Main">
          {portal && (
            <>
              <NavLink to="/directory" className={linkClass}>
                Directory
              </NavLink>
              {session.empNo !== null && (
                <NavLink to={`/employees/${session.empNo}`} className={linkClass}>
                  My profile
                </NavLink>
              )}
              {session.empNo !== null && (
                <NavLink to={`/org/${session.empNo}`} className={linkClass}>
                  Org chart
                </NavLink>
              )}
              <NavLink to="/documents" className={linkClass}>
                Documents
              </NavLink>
            </>
          )}
          {isHrAdmin(session) && (
            <NavLink to="/export" className={linkClass}>
              Export
            </NavLink>
          )}
        </nav>
        <div className="user-menu">
          <span className="muted" aria-label="Signed in as">
            {session.username}
          </span>
          <button type="button" className="button button-secondary" onClick={session.signOut}>
            Sign out
          </button>
        </div>
      </header>
      <main id="main" className="page" tabIndex={-1}>
        <Outlet />
      </main>
    </>
  );
}
