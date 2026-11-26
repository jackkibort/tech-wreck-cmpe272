import type { TitleRecord } from '../shared/api/types';
import { formatToDate } from '../shared/format';

/** Title history table, oldest first as returned by the API. */
export function TitleHistory({ titles }: { titles: TitleRecord[] }) {
  if (titles.length === 0) {
    return <p>No title history.</p>;
  }
  return (
    <table className="table">
      <caption className="sr-only">Title history</caption>
      <thead>
        <tr>
          <th scope="col">Title</th>
          <th scope="col">From</th>
          <th scope="col">To</th>
        </tr>
      </thead>
      <tbody>
        {titles.map((record) => (
          <tr key={`${record.title}-${record.fromDate}`}>
            <td>{record.title}</td>
            <td>{record.fromDate}</td>
            <td>{formatToDate(record.toDate)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  );
}
