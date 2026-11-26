import { vi } from 'vitest';

import { ApiClient } from '../shared/api/client';

/** A canned response for one API path. */
export interface FakeResponse {
  status?: number;
  body?: unknown;
  headers?: Record<string, string>;
}

/** Maps "GET /api/me" (method + path without query) to a response or a function returning one. */
export type Routes = Record<string, FakeResponse | ((url: URL) => FakeResponse)>;

/**
 * Builds an ApiClient whose fetch answers from `routes`. Unknown paths answer 404.
 * Returns the client, the fetch spy (to assert which requests were made) and the 401 callback.
 */
function toUrl(input: RequestInfo | URL): URL {
  const href = input instanceof Request ? input.url : input.toString();
  return new URL(href, 'https://securehr.local');
}

export function fakeApi(routes: Routes) {
  const onUnauthorized = vi.fn();
  const fetchFn = vi.fn((input: RequestInfo | URL, init?: RequestInit) => {
    const url = toUrl(input);
    const key = `${init?.method ?? 'GET'} ${url.pathname}`;
    const route = routes[key];
    const response: FakeResponse =
      route === undefined
        ? {
            status: 404,
            body: { status: 404, error: 'Not Found', message: 'Not found.', requestId: 'r-404' },
          }
        : typeof route === 'function'
          ? route(url)
          : route;
    const status = response.status ?? 200;
    // 204 and 304 responses may not carry a body.
    const body =
      status === 204 || status === 304
        ? null
        : typeof response.body === 'string'
          ? response.body
          : JSON.stringify(response.body ?? null);
    return Promise.resolve(
      new Response(body, {
        status,
        headers: { 'Content-Type': 'application/json', ...response.headers },
      }),
    );
  });
  const client = new ApiClient({
    getAccessToken: () => Promise.resolve('test-token'),
    onUnauthorized,
    fetchFn: fetchFn,
  });
  /** Paths (with query) requested so far, e.g. "GET /api/employees?page=0". */
  const requested = () =>
    fetchFn.mock.calls.map(([input, init]) => {
      const url = toUrl(input);
      return `${init?.method ?? 'GET'} ${url.pathname}${url.search}`;
    });
  return { client, fetchFn, onUnauthorized, requested };
}

/** The API's error body (Section 0.6). */
export function errorBody(status: number, message: string) {
  return { status, body: { status, error: 'Error', message, requestId: `req-${status}` } };
}
