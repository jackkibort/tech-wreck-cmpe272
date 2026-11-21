// G3 check: directory search on the full dataset, p95 < 300 ms. Local harness, not the CI suite.
import http from 'k6/http';
import { check } from 'k6';

const tokens = JSON.parse(open('/perf/tokens.json'));
const BASE = 'http://api:8080/api/employees';
const NAMES = ['Fac', 'Sim', 'Bam', 'Kob', 'Mal', 'Pre', 'Zie', 'Kal', 'Pea', 'Geo', 'Par', 'Kyo',
  'Sa', 'Ma', 'Ch', 'Be', 'Ta', 'Ge', 'Ka', 'Ba'];
const DEPTS = ['d001', 'd002', 'd003', 'd004', 'd005', 'd006', 'd007', 'd008', 'd009'];
const pick = (list) => list[Math.floor(Math.random() * list.length)];

export const options = {
  scenarios: {
    name: { executor: 'constant-vus', vus: 5, duration: '60s', exec: 'byName' },
    department: { executor: 'constant-vus', vus: 5, duration: '60s', exec: 'byDepartment' },
    directory: { executor: 'constant-vus', vus: 2, duration: '60s', exec: 'defaultView' },
  },
  thresholds: {
    'http_req_duration{scenario:name}': ['p(95)<300'],
    'http_req_duration{scenario:department}': ['p(95)<300'],
    'http_req_duration{scenario:directory}': ['p(95)<300'],
    checks: ['rate==1'],
  },
};

const get = (query) => {
  const res = http.get(`${BASE}?${query}`, { headers: { Authorization: `Bearer ${tokens.employee}` } });
  check(res, { 'status 200': (r) => r.status === 200 });
};

export function byName() { get(`q=${pick(NAMES)}&page=${Math.floor(Math.random() * 3)}`); }
export function byDepartment() { get(`deptNo=${pick(DEPTS)}&page=${Math.floor(Math.random() * 20)}`); }
export function defaultView() { get(`page=${Math.floor(Math.random() * 50)}`); }
