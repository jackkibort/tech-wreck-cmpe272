import { describe, expect, it, vi } from 'vitest';

import { errorBody, fakeApi } from '../../test/fakeApi';

import { ApiClient, ApiError, fileNameFrom, toQueryString } from './client';

describe('ApiClient', () => {
  it('sends the bearer token and a request ID, and omits cookies', async () => {
    const { client, fetchFn } = fakeApi({ 'GET /api/me': { body: { username: 'demo.employee' } } });

    await client.getJson('/api/me');

    const [, init] = fetchFn.mock.calls[0]!;
    const headers = new Headers(init?.headers);
    expect(headers.get('Authorization')).toBe('Bearer test-token');
    expect(headers.get('X-Request-ID')).toMatch(/^[0-9a-f-]{36}$/);
    expect(init?.credentials).toBe('omit');
  });

  it('sends no Authorization header when there is no token', async () => {
    const fetchFn = vi.fn(() => Promise.resolve(new Response('{}', { status: 200 })));
    const client = new ApiClient({
      getAccessToken: () => Promise.resolve(undefined),
      onUnauthorized: vi.fn(),
      fetchFn: fetchFn,
    });

    await client.getJson('/api/me');

    const init = (fetchFn.mock.calls[0] as unknown as [string, RequestInit])[1];
    expect(new Headers(init.headers).has('Authorization')).toBe(false);
  });

  it('turns an error response into an ApiError with the API message and request ID', async () => {
    const { client } = fakeApi({
      'GET /api/employees/10041/salaries': errorBody(403, 'Not allowed.'),
    });

    const error = await client.getJson('/api/employees/10041/salaries').catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 403, message: 'Not allowed.', requestId: 'req-403' });
  });

  it('calls onUnauthorized on 401 so the session can sign in again', async () => {
    const { client, onUnauthorized } = fakeApi({ 'GET /api/me': errorBody(401, 'Expired.') });

    await expect(client.getJson('/api/me')).rejects.toMatchObject({ status: 401 });
    expect(onUnauthorized).toHaveBeenCalledTimes(1);
  });

  it('uses a default message and the X-Request-ID header when the body is not JSON', async () => {
    const { client } = fakeApi({
      'GET /api/employees': {
        status: 429,
        body: '<html>busy</html>',
        headers: { 'X-Request-ID': 'edge-1' },
      },
    });

    await expect(client.getJson('/api/employees')).rejects.toMatchObject({
      status: 429,
      message: 'Too many requests.',
      requestId: 'edge-1',
    });
  });

  it('reports a network failure as status 0', async () => {
    const client = new ApiClient({
      getAccessToken: () => Promise.resolve('t'),
      onUnauthorized: vi.fn(),
      fetchFn: () => Promise.reject(new TypeError('Failed to fetch')),
    });

    await expect(client.getJson('/api/me')).rejects.toMatchObject({ status: 0 });
  });

  it('downloads files with the name from Content-Disposition', async () => {
    const { client } = fakeApi({
      'GET /api/export/employees': {
        body: 'emp_no\n10012',
        headers: {
          'Content-Type': 'text/csv',
          'Content-Disposition': 'attachment; filename="employees-export.csv"',
        },
      },
    });

    const file = await client.getBlob('/api/export/employees', { deptNo: 'd005' }, 'fallback.csv');

    expect(file.fileName).toBe('employees-export.csv');
    expect(await file.blob.text()).toBe('emp_no\n10012');
  });
});

describe('toQueryString', () => {
  it('leaves out undefined, null and empty values', () => {
    expect(toQueryString({ q: 'Bri', deptNo: '', title: undefined, hiredTo: null, page: 0 })).toBe(
      '?q=Bri&page=0',
    );
    expect(toQueryString(undefined)).toBe('');
  });
});

describe('fileNameFrom', () => {
  it('falls back when the header is missing', () => {
    expect(fileNameFrom(null, 'download.pdf')).toBe('download.pdf');
    expect(fileNameFrom('attachment; filename=payslip.pdf', 'x')).toBe('payslip.pdf');
  });
});
