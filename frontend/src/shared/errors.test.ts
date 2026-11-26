import { describe, expect, it } from 'vitest';

import { ApiError } from './api/client';
import { explainError } from './errors';

describe('TC-UI-02 explainError', () => {
  it('401: tells the user they are being signed in again, no retry button', () => {
    expect(explainError(new ApiError(401, 'x', null))).toEqual({
      title: 'Your session has expired',
      detail: 'Signing you in again…',
      retry: false,
    });
  });

  it('403: shows the API message without a retry', () => {
    expect(explainError(new ApiError(403, 'You may not view this salary.', 'r'))).toEqual({
      title: 'Access denied',
      detail: 'You may not view this salary.',
      retry: false,
    });
  });

  it('429: asks the user to wait and offers a retry', () => {
    const explained = explainError(new ApiError(429, 'x', null));
    expect(explained.title).toBe('Too many requests');
    expect(explained.detail).toMatch(/wait a few seconds/i);
    expect(explained.retry).toBe(true);
  });

  it('offers a retry for server and network errors but not for other client errors', () => {
    expect(explainError(new ApiError(503, 'down', null)).retry).toBe(true);
    expect(explainError(new ApiError(0, 'offline', null)).retry).toBe(true);
    expect(explainError(new ApiError(400, 'bad', null)).retry).toBe(false);
    expect(explainError(new Error('boom')).title).toBe('Something went wrong');
  });
});
