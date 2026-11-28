// TC-SEC-03: a burst of requests beyond the edge's per-IP rate limit must get 429 responses,
// and the API must stay up for the requests that are let through.
// Runs against the edge only (BASE_URL=https://securehr.local); the API itself has no limit.
import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';
import { accessToken } from '../lib/auth.js';
import { summaryWriter } from '../lib/summary.js';

const BASE_URL = __ENV.BASE_URL || 'https://securehr.local';
const ISSUER = __ENV.KEYCLOAK_ISSUER_URI || 'https://auth.securehr.local/realms/securehr';
const RATE = Number(__ENV.BURST_RATE || 200);

const limited = new Counter('rate_limited_responses');
const serverErrors = new Counter('server_error_responses');

export const options = {
  scenarios: {
    // A fixed arrival rate well above any sane per-IP limit, from one source address.
    burst: {
      executor: 'constant-arrival-rate',
      rate: RATE,
      timeUnit: '1s',
      duration: __ENV.DURATION || '15s',
      preAllocatedVUs: 50,
      maxVUs: 200,
    },
  },
  thresholds: {
    'http_req_duration{scenario:burst}': ['p(95)<2000'],
    'http_reqs{scenario:burst}': ['count>0'],
    rate_limited_responses: ['count>0'],
    server_error_responses: ['count==0'],
    checks: ['rate==1'],
  },
};

export function setup() {
  return { token: accessToken(BASE_URL, ISSUER) };
}

export default function (data) {
  const res = http.get(`${BASE_URL}/api/employees?page=0`, {
    headers: { Authorization: `Bearer ${data.token}` },
    timeout: '5s',
    tags: { name: 'GET /api/employees' },
  });
  if (res.status === 429) limited.add(1);
  if (res.status >= 500) serverErrors.add(1);
  check(res, { 'allowed (200) or limited (429)': (r) => r.status === 200 || r.status === 429 });
}

export const handleSummary = summaryWriter('burst', ['burst'], 'TC-SEC-03 request burst', [
  'rate_limited_responses',
  'server_error_responses',
]);
