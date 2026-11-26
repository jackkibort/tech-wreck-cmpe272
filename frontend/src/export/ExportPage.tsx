import { useState } from 'react';

import { isHrAdmin } from '../auth/roles';
import { useSession } from '../auth/session';
import { SearchFilters } from '../employees/SearchFilters';
import { useApiClient } from '../shared/api/apiClientContext';
import type { EmployeeFilters } from '../shared/api/types';
import { ErrorMessage } from '../shared/components/ErrorMessage';
import { saveBlob } from '../shared/format';

type Status =
  | { kind: 'idle' }
  | { kind: 'working' }
  | { kind: 'done'; fileName: string; truncated: boolean }
  | { kind: 'failed'; error: unknown };

/** UC-05: HR Admin exports a filtered list to CSV (capped at 10,000 rows, audited as EXPORT). */
export function ExportPage() {
  const session = useSession();
  const client = useApiClient();
  const [status, setStatus] = useState<Status>({ kind: 'idle' });

  if (!isHrAdmin(session)) {
    return (
      <p role="alert" className="alert alert-error">
        CSV export is available to HR Admins only.
      </p>
    );
  }

  const runExport = async (filters: EmployeeFilters) => {
    setStatus({ kind: 'working' });
    try {
      const file = await client.getBlob(
        '/api/export/employees',
        { ...filters },
        'employees-export.csv',
      );
      saveBlob(file.blob, file.fileName);
      setStatus({
        kind: 'done',
        fileName: file.fileName,
        truncated: file.headers.get('X-Export-Truncated') === 'true',
      });
    } catch (error) {
      setStatus({ kind: 'failed', error });
    }
  };

  return (
    <section aria-labelledby="export-title">
      <h1 id="export-title">Export employees</h1>
      <p>
        Downloads the employees matching these filters as CSV, up to 10,000 rows. Every export is
        recorded in the audit log.
      </p>
      <SearchFilters
        initial={{}}
        submitLabel={status.kind === 'working' ? 'Exporting…' : 'Export CSV'}
        onSubmit={(filters) => void runExport(filters)}
      />
      <div aria-live="polite">
        {status.kind === 'done' && (
          <p className="alert alert-success">
            Downloaded {status.fileName}.
            {status.truncated &&
              ' The result was cut off at 10,000 rows; narrow the filters to get the rest.'}
          </p>
        )}
      </div>
      {status.kind === 'failed' && <ErrorMessage error={status.error} />}
    </section>
  );
}
