import { describe, expect, it } from 'vitest';

import { canSeeSalary, hasPortalRole, isHrAdmin } from './roles';

describe('role helpers (display only; the API enforces access)', () => {
  it('shows salary to the employee themself and to HR Admins only (Section 8.2)', () => {
    expect(canSeeSalary({ roles: ['EMPLOYEE'], empNo: 10012 }, 10012)).toBe(true);
    expect(canSeeSalary({ roles: ['EMPLOYEE'], empNo: 10012 }, 10041)).toBe(false);
    expect(canSeeSalary({ roles: ['EMPLOYEE', 'MANAGER'], empNo: 110567 }, 10012)).toBe(false);
    expect(canSeeSalary({ roles: ['EMPLOYEE', 'HR_ADMIN'], empNo: 10005 }, 10012)).toBe(true);
  });

  it('treats only EMPLOYEE, MANAGER and HR_ADMIN as portal roles', () => {
    expect(hasPortalRole({ roles: ['AUDITOR'] })).toBe(false);
    expect(hasPortalRole({ roles: ['SECUREHR_DEV'] })).toBe(false);
    expect(hasPortalRole({ roles: ['EMPLOYEE'] })).toBe(true);
    expect(isHrAdmin({ roles: ['EMPLOYEE', 'MANAGER'] })).toBe(false);
  });
});
