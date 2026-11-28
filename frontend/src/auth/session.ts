import { createContext, useContext } from 'react';

import type { EmployeeSummary, Role } from '../shared/api/types';

/**
 * The signed-in user as the portal sees it. Built from GET /api/me, so roles and emp_no are the
 * ones the API derived from the validated token, not values the UI decoded itself.
 */
export interface Session {
  username: string;
  empNo: number | null;
  roles: readonly Role[];
  employee: EmployeeSummary | null;
  signOut: () => void;
}

export const SessionContext = createContext<Session | null>(null);

/**
 * Returns the current session.
 * @throws when used outside a signed-in part of the app.
 */
export function useSession(): Session {
  const session = useContext(SessionContext);
  if (!session) {
    throw new Error('useSession must be used inside a signed-in session');
  }
  return session;
}
