import { Link, useParams } from 'react-router';

import { parseEmpNo } from '../employees/empNo';
import type { ApiClient } from '../shared/api/client';
import type { OrgChain } from '../shared/api/types';
import { ErrorMessage } from '../shared/components/ErrorMessage';
import { Loading } from '../shared/components/Loading';
import { fullName } from '../shared/format';
import { useApiResource } from '../shared/hooks/useApiResource';

function getOrgChain(client: ApiClient, empNo: number, signal: AbortSignal) {
  return client.getJson<OrgChain>(`/api/org/${empNo}`, undefined, signal);
}

/** Org chart: the management chain from an employee up to their department manager. */
export function OrgChartPage() {
  const empNo = parseEmpNo(useParams().empNo);
  const chain = useApiResource(empNo === null ? null : `org/${empNo}`, (client, signal) =>
    getOrgChain(client, empNo ?? 0, signal),
  );

  if (empNo === null) {
    return <p role="alert">That is not a valid employee number.</p>;
  }
  if (chain.loading) {
    return <Loading label="Loading org chart…" />;
  }
  if (chain.error !== undefined || !chain.data) {
    return <ErrorMessage error={chain.error} onRetry={chain.reload} />;
  }

  // The API lists the employee first and the manager last; show the manager at the top.
  const nodes = [...chain.data.chain].reverse();
  return (
    <section aria-labelledby="org-title">
      <h1 id="org-title">Org chart</h1>
      {chain.data.deptName && <p className="muted">Department: {chain.data.deptName}</p>}
      <ol className="org-chain">
        {nodes.map((node, index) => (
          <li key={node.empNo} className={node.empNo === empNo ? 'org-node current' : 'org-node'}>
            <Link to={`/employees/${node.empNo}`}>{fullName(node)}</Link>
            <span className="muted">{node.title ?? '—'}</span>
            {index === 0 && nodes.length > 1 && <span className="badge">Department manager</span>}
          </li>
        ))}
      </ol>
    </section>
  );
}
