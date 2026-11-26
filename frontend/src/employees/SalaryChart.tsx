import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';

import type { SalaryRecord } from '../shared/api/types';
import { formatSalary, formatToDate } from '../shared/format';

/**
 * Salary history as a step chart, with the same numbers in a table below it so the data is
 * available to screen readers and keyboard users (Section 12.3).
 */
export function SalaryChart({ salaries }: { salaries: SalaryRecord[] }) {
  if (salaries.length === 0) {
    return <p>No salary history.</p>;
  }
  const points = salaries.map((record) => ({ date: record.fromDate, salary: record.salary }));

  return (
    <>
      <div className="chart" aria-hidden="true">
        <ResponsiveContainer width="100%" height={260}>
          <LineChart data={points} margin={{ top: 8, right: 16, bottom: 8, left: 16 }}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" minTickGap={24} />
            <YAxis tickFormatter={(value: number) => formatSalary(value)} width={80} />
            <Tooltip formatter={(value) => formatSalary(Number(value))} />
            <Line type="stepAfter" dataKey="salary" stroke="#1d4ed8" strokeWidth={2} dot={false} />
          </LineChart>
        </ResponsiveContainer>
      </div>
      <table className="table">
        <caption>Salary history</caption>
        <thead>
          <tr>
            <th scope="col">Salary</th>
            <th scope="col">From</th>
            <th scope="col">To</th>
          </tr>
        </thead>
        <tbody>
          {salaries.map((record) => (
            <tr key={record.fromDate}>
              <td>{formatSalary(record.salary)}</td>
              <td>{record.fromDate}</td>
              <td>{formatToDate(record.toDate)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </>
  );
}
