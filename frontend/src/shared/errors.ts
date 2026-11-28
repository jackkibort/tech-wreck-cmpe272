import { ApiError } from './api/client';

export interface Explained {
  title: string;
  detail: string;
  retry: boolean;
}

/** Turns an error into the friendly message the UI shows (TC-UI-02). */
export function explainError(error: unknown): Explained {
  if (!(error instanceof ApiError)) {
    return { title: 'Something went wrong', detail: 'An unexpected error occurred.', retry: true };
  }
  switch (error.status) {
    case 401:
      return {
        title: 'Your session has expired',
        detail: 'Signing you in again…',
        retry: false,
      };
    case 403:
      return { title: 'Access denied', detail: error.message, retry: false };
    case 404:
      return { title: 'Not found', detail: error.message, retry: false };
    case 429:
      return {
        title: 'Too many requests',
        detail: 'You are sending requests too quickly. Wait a few seconds, then try again.',
        retry: true,
      };
    case 0:
      return { title: 'Connection problem', detail: error.message, retry: true };
    default:
      return { title: 'Something went wrong', detail: error.message, retry: error.status >= 500 };
  }
}
