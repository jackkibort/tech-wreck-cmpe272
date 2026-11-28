import { Navigate, Route, Routes } from 'react-router';

import { hasPortalRole } from './auth/roles';
import { useSession } from './auth/session';
import { DirectoryPage } from './employees/DirectoryPage';
import { DocumentsPage } from './documents/DocumentsPage';
import { EmployeePage } from './employees/EmployeePage';
import { ExportPage } from './export/ExportPage';
import { OrgChartPage } from './org/OrgChartPage';
import { Layout } from './shared/components/Layout';

/** All signed-in routes. Each page still relies on the API for authorization. */
export function AppRoutes() {
  const session = useSession();
  const home = hasPortalRole(session) ? '/directory' : '/export';

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<Navigate to={home} replace />} />
        <Route path="directory" element={<DirectoryPage />} />
        <Route path="employees/:empNo" element={<EmployeePage />} />
        <Route path="org/:empNo" element={<OrgChartPage />} />
        <Route path="documents" element={<DocumentsPage />} />
        <Route path="export" element={<ExportPage />} />
        <Route path="*" element={<p role="alert">Page not found.</p>} />
      </Route>
    </Routes>
  );
}
