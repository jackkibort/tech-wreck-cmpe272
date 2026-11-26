import { Link, useSearchParams } from 'react-router';

import { ErrorMessage } from '../shared/components/ErrorMessage';
import { Loading } from '../shared/components/Loading';
import { Pagination } from '../shared/components/Pagination';
import { useApiResource } from '../shared/hooks/useApiResource';

import { searchEmployees } from './api';
import { filtersFromParams, filtersToParams, pageFromParams } from './filters';
import { SearchFilters } from './SearchFilters';

/** UC-02: search and browse the directory (names, department and current title only). */
export function DirectoryPage() {
  const [params, setParams] = useSearchParams();
  const filters = filtersFromParams(params);
  const page = pageFromParams(params);
  const key = `search?${filtersToParams(filters, page).toString()}`;

  const results = useApiResource(key, (client, signal) =>
    searchEmployees(client, filters, page, signal),
  );

  return (
    <section aria-labelledby="directory-title">
      <h1 id="directory-title">Employee directory</h1>
      <SearchFilters
        // Remount when the URL changes (Back button) so the form shows the active filters.
        key={key}
        initial={filters}
        submitLabel="Search"
        onSubmit={(next) => {
          setParams(filtersToParams(next));
        }}
      />

      {results.loading && <Loading label="Searching…" />}
      {results.error !== undefined && (
        <ErrorMessage error={results.error} onRetry={results.reload} />
      )}
      {results.data && (
        <>
          {results.data.content.length === 0 ? (
            <p>No employees match these filters.</p>
          ) : (
            <table className="table">
              <caption className="sr-only">Search results</caption>
              <thead>
                <tr>
                  <th scope="col">Name</th>
                  <th scope="col">Department</th>
                  <th scope="col">Current title</th>
                  <th scope="col">Employee no.</th>
                </tr>
              </thead>
              <tbody>
                {results.data.content.map((employee) => (
                  <tr key={employee.empNo}>
                    <td>
                      <Link to={`/employees/${employee.empNo}`}>
                        {employee.lastName}, {employee.firstName}
                      </Link>
                    </td>
                    <td>{employee.deptName ?? '—'}</td>
                    <td>{employee.title ?? '—'}</td>
                    <td>{employee.empNo}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
          <Pagination
            page={results.data.page}
            totalPages={results.data.totalPages}
            totalElements={results.data.totalElements}
            onPageChange={(next) => {
              setParams(filtersToParams(filters, next));
            }}
          />
        </>
      )}
    </section>
  );
}
