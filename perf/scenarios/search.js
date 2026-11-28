// G3 load test (Section 2): directory search over the 300K-employee dataset must keep its
// 95th-percentile response time under 300 ms. Run it twice to compare:
//   through the edge (firewall on):  BASE_URL=https://securehr.local
//   straight to the API (no edge):   BASE_URL=http://api:8080
// The same search mix as the Employee API's own check (backend/perf-check), so results line up.
import http from 'k6/http';
import { check } from 'k6';
import { accessToken } from '../lib/auth.js';
import { summaryWriter } from '../lib/summary.js';

const BASE_URL = __ENV.BASE_URL || 'https://securehr.local';
const ISSUER = __ENV.KEYCLOAK_ISSUER_URI || 'https://auth.securehr.local/realms/securehr';
const DURATION = __ENV.DURATION || '60s';

const NAMES = ['Fac', 'Sim', 'Bam', 'Kob', 'Mal', 'Pre', 'Zie', 'Kal', 'Pea', 'Geo', 'Par', 'Kyo',
  'Sa', 'Ma', 'Ch', 'Be', 'Ta', 'Ge', 'Ka', 'Ba'];
const DEPTS = ['d001', 'd002', 'd003', 'd004', 'd005', 'd006', 'd007', 'd008', 'd009'];
const pick = (list) => list[Math.floor(Math.random() * list.length)];

export const options = {
  // The edge certificate comes from the local mkcert authority; run.sh makes k6 trust it.
  scenarios: {
    name: { executor: 'constant-vus', vus: 5, duration: DURATION, exec: 'byName' },
    department: { executor: 'constant-vus', vus: 5, duration: DURATION, exec: 'byDepartment' },
    directory: { executor: 'constant-vus', vus: 2, duration: DURATION, exec: 'defaultView' },
  },
  thresholds: {
    'http_req_duration{scenario:name}': ['p(95)<300'],
    'http_req_duration{scenario:department}': ['p(95)<300'],
    'http_req_duration{scenario:directory}': ['p(95)<300'],
    'http_reqs{scenario:name}': ['count>0'],
    'http_reqs{scenario:department}': ['count>0'],
    'http_reqs{scenario:directory}': ['count>0'],
    checks: ['rate==1'],
  },
};

export function setup() {
  return { token: accessToken(BASE_URL, ISSUER) };
}

function search(data, query) {
  const res = http.get(`${BASE_URL}/api/employees?${query}`, {
    headers: { Authorization: `Bearer ${data.token}` },
    // Every outgoing call has a timeout; a hung request counts as a failure, not a wait.
    timeout: '5s',
    tags: { name: 'GET /api/employees' },
  });
  check(res, { 'status 200': (r) => r.status === 200 });
}

export function byName(data) { search(data, `q=${pick(NAMES)}&page=${Math.floor(Math.random() * 3)}`); }
export function byDepartment(data) { search(data, `deptNo=${pick(DEPTS)}&page=${Math.floor(Math.random() * 20)}`); }
export function defaultView(data) { search(data, `page=${Math.floor(Math.random() * 50)}`); }

export const handleSummary = summaryWriter(
  `search-${__ENV.TARGET_LABEL || 'run'}`,
  ['name', 'department', 'directory'],
  'G3 directory search',
);
