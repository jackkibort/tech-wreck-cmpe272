/**
 * Role checks used only to decide what to SHOW. They never grant access: the APIs enforce the
 * Section 8.2 and 10.2 matrices on every request, and the UI shows only what they return.
 */
import type { Role } from '../shared/api/types';

import type { Session } from './session';

/** True when the session holds the role. */
export function hasRole(session: Pick<Session, 'roles'>, role: Role): boolean {
  return session.roles.includes(role);
}

/** CSV export and document upload/delete are HR Admin features (Sections 8.2, 10.2). */
export function isHrAdmin(session: Pick<Session, 'roles'>): boolean {
  return hasRole(session, 'HR_ADMIN');
}

/** Salary history is visible to the employee themself and to HR Admins (Section 8.2). */
export function canSeeSalary(session: Pick<Session, 'roles' | 'empNo'>, empNo: number): boolean {
  return session.empNo === empNo || isHrAdmin(session);
}

/** The portal's business roles; AUDITOR and SECUREHR_DEV alone get no portal pages. */
export function hasPortalRole(session: Pick<Session, 'roles'>): boolean {
  return (['EMPLOYEE', 'MANAGER', 'HR_ADMIN'] as const).some((role) => hasRole(session, role));
}
