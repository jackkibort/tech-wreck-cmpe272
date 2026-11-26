import type { EmployeeFilters } from '../shared/api/types';

const FILTER_KEYS = ['q', 'deptNo', 'title', 'hiredFrom', 'hiredTo'] as const;

/** Reads the search filters from the URL, so searches can be bookmarked and the Back button works. */
export function filtersFromParams(params: URLSearchParams): EmployeeFilters {
  const filters: EmployeeFilters = {};
  for (const key of FILTER_KEYS) {
    const value = params.get(key)?.trim();
    if (value) {
      filters[key] = value;
    }
  }
  return filters;
}

/** Writes filters (and a page number) back into URL parameters, leaving out empty values. */
export function filtersToParams(filters: EmployeeFilters, page = 0): URLSearchParams {
  const params = new URLSearchParams();
  for (const key of FILTER_KEYS) {
    const value = filters[key]?.trim();
    if (value) {
      params.set(key, value);
    }
  }
  if (page > 0) {
    params.set('page', String(page));
  }
  return params;
}

/** Zero-based page from the URL; anything invalid becomes 0. */
export function pageFromParams(params: URLSearchParams): number {
  const page = Number.parseInt(params.get('page') ?? '0', 10);
  return Number.isFinite(page) && page > 0 ? page : 0;
}
