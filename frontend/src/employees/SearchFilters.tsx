import { useState, type SubmitEvent } from 'react';

import type { EmployeeFilters } from '../shared/api/types';
import { useApiResource } from '../shared/hooks/useApiResource';

import { listDepartments } from './api';

interface Props {
  initial: EmployeeFilters;
  submitLabel: string;
  onSubmit: (filters: EmployeeFilters) => void;
}

/**
 * Directory filters (Section 0.6): name prefix, department, exact title and hire-date range.
 * Shared by the directory search and the HR export, which accept the same filters.
 * Input limits mirror the API's validation so most mistakes are caught before a request.
 */
export function SearchFilters({ initial, submitLabel, onSubmit }: Props) {
  const [filters, setFilters] = useState<EmployeeFilters>(initial);
  const departments = useApiResource('departments', (client, signal) =>
    listDepartments(client, signal),
  );

  const update = (key: keyof EmployeeFilters) => (value: string) => {
    setFilters((current) => ({ ...current, [key]: value }));
  };

  const submit = (event: SubmitEvent) => {
    event.preventDefault();
    onSubmit(filters);
  };

  return (
    <form className="filters" onSubmit={submit} aria-label="Search filters">
      <label>
        Name starts with
        <input
          type="search"
          name="q"
          maxLength={16}
          value={filters.q ?? ''}
          onChange={(event) => {
            update('q')(event.target.value);
          }}
          placeholder="e.g. Bridgland"
        />
      </label>
      <label>
        Department
        <select
          name="deptNo"
          value={filters.deptNo ?? ''}
          onChange={(event) => {
            update('deptNo')(event.target.value);
          }}
        >
          <option value="">All departments</option>
          {departments.data?.map((department) => (
            <option key={department.deptNo} value={department.deptNo}>
              {department.deptName}
            </option>
          ))}
        </select>
      </label>
      <label>
        Current title
        <input
          type="text"
          name="title"
          maxLength={50}
          value={filters.title ?? ''}
          onChange={(event) => {
            update('title')(event.target.value);
          }}
          placeholder="e.g. Senior Engineer"
        />
      </label>
      <label>
        Hired from
        <input
          type="date"
          name="hiredFrom"
          value={filters.hiredFrom ?? ''}
          onChange={(event) => {
            update('hiredFrom')(event.target.value);
          }}
        />
      </label>
      <label>
        Hired to
        <input
          type="date"
          name="hiredTo"
          value={filters.hiredTo ?? ''}
          onChange={(event) => {
            update('hiredTo')(event.target.value);
          }}
        />
      </label>
      <div className="filters-actions">
        <button type="submit" className="button">
          {submitLabel}
        </button>
      </div>
    </form>
  );
}
