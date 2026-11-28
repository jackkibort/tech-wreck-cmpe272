/**
 * Response types for the Employee API, matching api-spec/employee-api.yaml.
 * Optional properties are the masked fields: the API omits them for callers who may not see them.
 */

/** Realm roles from Keycloak (Section 0.5). */
export type Role = 'EMPLOYEE' | 'MANAGER' | 'HR_ADMIN' | 'AUDITOR' | 'SECUREHR_DEV';

/** Error body returned by both APIs and by the edge (Section 0.6). */
export interface ApiErrorBody {
  status: number;
  error: string;
  message: string;
  requestId: string;
}

export interface EmployeeSummary {
  empNo: number;
  firstName: string;
  lastName: string;
  deptNo?: string | null;
  deptName?: string | null;
  title?: string | null;
}

export interface Me {
  username: string;
  empNo?: number | null;
  roles: Role[];
  employee?: EmployeeSummary | null;
}

/** Paged response fields (Section 0.6). */
export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface EmployeeDetail {
  empNo: number;
  firstName: string;
  lastName: string;
  gender: 'M' | 'F';
  hireDate: string;
  deptNo?: string | null;
  deptName?: string | null;
  title?: string | null;
  /** Present only for the employee themself and HR_ADMIN. */
  birthDate?: string;
  /** Present only for the employee themself and HR_ADMIN. */
  currentSalary?: number;
}

export interface TitleRecord {
  title: string;
  fromDate: string;
  toDate?: string | null;
}

export interface SalaryRecord {
  salary: number;
  fromDate: string;
  toDate?: string | null;
}

export interface EmployeeRef {
  empNo: number;
  firstName: string;
  lastName: string;
}

export interface Department {
  deptNo: string;
  deptName: string;
  manager?: EmployeeRef | null;
}

export interface OrgNode {
  empNo: number;
  firstName: string;
  lastName: string;
  title?: string | null;
}

export interface OrgChain {
  deptNo?: string | null;
  deptName?: string | null;
  chain: OrgNode[];
}

/** Search filters shared by /api/employees and /api/export/employees (Section 0.6). */
export interface EmployeeFilters {
  q?: string;
  deptNo?: string;
  title?: string;
  hiredFrom?: string;
  hiredTo?: string;
}
