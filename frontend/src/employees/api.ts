/** Employee API calls (api-spec/employee-api.yaml). */
import type { ApiClient } from '../shared/api/client';
import type {
  Department,
  EmployeeDetail,
  EmployeeFilters,
  EmployeeSummary,
  Page,
  SalaryRecord,
  TitleRecord,
} from '../shared/api/types';

export const DEFAULT_PAGE_SIZE = 25;

export function searchEmployees(
  client: ApiClient,
  filters: EmployeeFilters,
  page: number,
  signal?: AbortSignal,
): Promise<Page<EmployeeSummary>> {
  return client.getJson<Page<EmployeeSummary>>(
    '/api/employees',
    { ...filters, page, size: DEFAULT_PAGE_SIZE },
    signal,
  );
}

export function getEmployee(client: ApiClient, empNo: number, signal?: AbortSignal) {
  return client.getJson<EmployeeDetail>(`/api/employees/${empNo}`, undefined, signal);
}

export function getTitles(client: ApiClient, empNo: number, signal?: AbortSignal) {
  return client.getJson<TitleRecord[]>(`/api/employees/${empNo}/titles`, undefined, signal);
}

/** Every call is audited as SALARY_READ by the API (Section 9). */
export function getSalaries(client: ApiClient, empNo: number, signal?: AbortSignal) {
  return client.getJson<SalaryRecord[]>(`/api/employees/${empNo}/salaries`, undefined, signal);
}

export function listDepartments(client: ApiClient, signal?: AbortSignal) {
  return client.getJson<Department[]>('/api/departments', undefined, signal);
}
