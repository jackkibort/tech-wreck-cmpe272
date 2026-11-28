import type { APIRequestContext } from '@playwright/test';
import { urls } from './env';

/** One audit event as written by the APIs and the edge (Section 11.1). */
export type AuditEvent = Record<string, unknown>;

/** The exact audit field set every producer must write (Section 11.1). */
export const AUDIT_FIELDS = [
  'timestamp',
  'event_type',
  'actor_username',
  'actor_emp_no',
  'actor_roles',
  'target_emp_no',
  'resource',
  'outcome',
  'source_ip',
  'request_id',
] as const;

interface Frame {
  schema?: { fields?: { name: string }[] };
  data?: { values?: unknown[][] };
}

/**
 * Reads audit lines from Loki through Grafana's query API, using the signed-in Grafana session
 * of the request context. Returns the parsed JSON of every matching line in [from, to].
 */
export async function auditEvents(
  request: APIRequestContext,
  logql: string,
  from: Date,
  to: Date,
): Promise<AuditEvent[]> {
  const response = await request.post(`${urls.grafana}/api/ds/query`, {
    data: {
      from: String(from.getTime()),
      to: String(to.getTime()),
      queries: [{ refId: 'A', datasource: { type: 'loki', uid: 'loki' }, expr: logql, queryType: 'range', maxLines: 1000 }],
    },
    timeout: 15_000,
  });
  if (!response.ok()) throw new Error(`Grafana query failed: ${response.status()}`);
  const body = (await response.json()) as { results?: { A?: { frames?: Frame[] } } };
  const events: AuditEvent[] = [];
  for (const frame of body.results?.A?.frames ?? []) {
    const fields = frame.schema?.fields ?? [];
    const lineColumn = fields.findIndex((f) => f.name === 'Line' || f.name === 'line');
    const lines = lineColumn >= 0 ? (frame.data?.values?.[lineColumn] ?? []) : [];
    for (const line of lines) events.push(JSON.parse(String(line)) as AuditEvent);
  }
  return events;
}
