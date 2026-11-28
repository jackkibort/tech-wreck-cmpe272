import { lazy, Suspense } from 'react';
import { Link, useParams } from 'react-router';

import { canSeeSalary } from '../auth/roles';
import { useSession } from '../auth/session';
import { ErrorMessage } from '../shared/components/ErrorMessage';
import { Loading } from '../shared/components/Loading';
import type { EmployeeDetail } from '../shared/api/types';
import { formatSalary, fullName } from '../shared/format';
import { useApiResource } from '../shared/hooks/useApiResource';

import { getEmployee, getSalaries, getTitles } from './api';
import { parseEmpNo } from './empNo';

// The charting library is large; load it only when a salary history is shown.
const SalaryChart = lazy(() =>
  import('./SalaryChart').then((module) => ({ default: module.SalaryChart })),
);
import { TitleHistory } from './TitleHistory';

/**
 * UC-03 / UC-04 / UC-05: employee detail, title history and (where allowed) salary history.
 * The API decides what is visible; this page renders only the fields it returns.
 */
export function EmployeePage() {
  const empNo = parseEmpNo(useParams().empNo);
  if (empNo === null) {
    return <p role="alert">That is not a valid employee number.</p>;
  }
  return <EmployeeView key={empNo} empNo={empNo} />;
}

function EmployeeView({ empNo }: { empNo: number }) {
  const session = useSession();
  const detail = useApiResource(`employee/${empNo}`, (client, signal) =>
    getEmployee(client, empNo, signal),
  );

  if (detail.loading) {
    return <Loading label="Loading employee…" />;
  }
  if (detail.error !== undefined || !detail.data) {
    return <ErrorMessage error={detail.error} onRetry={detail.reload} />;
  }
  const employee = detail.data;
  const self = session.empNo === empNo;

  return (
    <article aria-labelledby="employee-title">
      <h1 id="employee-title">
        {fullName(employee)}
        {self && <span className="badge">You</span>}
      </h1>
      <DetailCard employee={employee} />
      <p>
        <Link to={`/org/${empNo}`}>View management chain</Link>
      </p>

      <section aria-labelledby="titles-title">
        <h2 id="titles-title">Title history</h2>
        <Titles empNo={empNo} />
      </section>

      {/* Only requested when the caller may see it: every read is audited (SALARY_READ). */}
      {canSeeSalary(session, empNo) && (
        <section aria-labelledby="salary-title">
          <h2 id="salary-title">Salary history</h2>
          <Salaries empNo={empNo} />
        </section>
      )}
    </article>
  );
}

function DetailCard({ employee }: { employee: EmployeeDetail }) {
  return (
    <dl className="details card">
      <dt>Employee no.</dt>
      <dd>{employee.empNo}</dd>
      <dt>Department</dt>
      <dd>{employee.deptName ?? '—'}</dd>
      <dt>Current title</dt>
      <dd>{employee.title ?? '—'}</dd>
      <dt>Hire date</dt>
      <dd>{employee.hireDate}</dd>
      <dt>Gender</dt>
      <dd>{employee.gender}</dd>
      {employee.birthDate !== undefined && (
        <>
          <dt>Birth date</dt>
          <dd>{employee.birthDate}</dd>
        </>
      )}
      {employee.currentSalary !== undefined && (
        <>
          <dt>Current salary</dt>
          <dd>{formatSalary(employee.currentSalary)}</dd>
        </>
      )}
    </dl>
  );
}

function Titles({ empNo }: { empNo: number }) {
  const titles = useApiResource(`titles/${empNo}`, (client, signal) =>
    getTitles(client, empNo, signal),
  );
  if (titles.loading) {
    return <Loading label="Loading title history…" />;
  }
  if (titles.error !== undefined || !titles.data) {
    return <ErrorMessage error={titles.error} onRetry={titles.reload} />;
  }
  return <TitleHistory titles={titles.data} />;
}

function Salaries({ empNo }: { empNo: number }) {
  const salaries = useApiResource(`salaries/${empNo}`, (client, signal) =>
    getSalaries(client, empNo, signal),
  );
  if (salaries.loading) {
    return <Loading label="Loading salary history…" />;
  }
  if (salaries.error !== undefined || !salaries.data) {
    return <ErrorMessage error={salaries.error} onRetry={salaries.reload} />;
  }
  return (
    <Suspense fallback={<Loading label="Loading chart…" />}>
      <SalaryChart salaries={salaries.data} />
    </Suspense>
  );
}
