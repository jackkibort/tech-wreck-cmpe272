/**
 * The single API client for the portal (Section 12.3). Design pattern: Facade.
 *
 * Every request goes through {@link ApiClient.request}, which adds the access token and an
 * X-Request-ID, and turns any non-2xx response into an {@link ApiError}. Feature modules call
 * the typed helpers (`getJson`, `getBlob`, ...) instead of `fetch`, so token handling and error
 * handling live in one place.
 */
import type { ApiErrorBody } from './types';

/** A failed API call. `status` is the HTTP status, or 0 when the network request failed. */
export class ApiError extends Error {
  readonly status: number;
  readonly requestId: string | null;

  constructor(status: number, message: string, requestId: string | null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.requestId = requestId;
  }
}

/** Query parameters; undefined and empty values are left out of the URL. */
export type QueryParams = Record<string, string | number | undefined | null>;

/** A downloaded file. */
export interface FileDownload {
  blob: Blob;
  fileName: string;
  headers: Headers;
}

export interface ApiClientOptions {
  /** Resolves the current access token, or undefined when signed out. */
  getAccessToken: () => Promise<string | undefined>;
  /** Called once a 401 is received, so the session can sign the user in again. */
  onUnauthorized: () => void;
  /** Base URL; the APIs share the portal's origin behind the edge (Section 0.3). */
  baseUrl?: string;
  /** Injected for tests. */
  fetchFn?: typeof fetch;
}

/** Builds a query string from defined, non-empty parameters. */
export function toQueryString(params: QueryParams | undefined): string {
  if (!params) {
    return '';
  }
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value));
    }
  }
  const text = search.toString();
  return text ? `?${text}` : '';
}

/** Extracts the file name from a Content-Disposition header. */
export function fileNameFrom(disposition: string | null, fallback: string): string {
  const match = disposition ? /filename="?([^";]+)"?/i.exec(disposition) : null;
  return match?.[1] ?? fallback;
}

function isApiErrorBody(value: unknown): value is ApiErrorBody {
  return (
    typeof value === 'object' &&
    value !== null &&
    typeof (value as { message?: unknown }).message === 'string'
  );
}

/** Default message per status, used when the response has no JSON error body. */
function defaultMessage(status: number): string {
  switch (status) {
    case 401:
      return 'Your session has expired.';
    case 403:
      return 'You do not have permission to do this.';
    case 404:
      return 'Not found.';
    case 413:
      return 'The file is too large.';
    case 429:
      return 'Too many requests.';
    default:
      return `Request failed (${status}).`;
  }
}

export class ApiClient {
  private readonly options: ApiClientOptions;
  private readonly fetchFn: typeof fetch;

  constructor(options: ApiClientOptions) {
    this.options = options;
    this.fetchFn = options.fetchFn ?? window.fetch.bind(window);
  }

  /**
   * Sends a request with the bearer token and a fresh X-Request-ID.
   * @throws {ApiError} on a network failure or any non-2xx status.
   */
  async request(path: string, init: RequestInit = {}): Promise<Response> {
    const headers = new Headers(init.headers);
    const token = await this.options.getAccessToken();
    if (token) {
      headers.set('Authorization', `Bearer ${token}`);
    }
    headers.set('X-Request-ID', crypto.randomUUID());
    if (!headers.has('Accept')) {
      headers.set('Accept', 'application/json');
    }

    let response: Response;
    try {
      response = await this.fetchFn(`${this.options.baseUrl ?? ''}${path}`, {
        ...init,
        headers,
        // Tokens travel only in the Authorization header, never in cookies.
        credentials: 'omit',
      });
    } catch (cause) {
      if (cause instanceof DOMException && cause.name === 'AbortError') {
        throw cause;
      }
      throw new ApiError(0, 'The server could not be reached. Check your connection.', null);
    }

    if (!response.ok) {
      throw await this.toError(response);
    }
    return response;
  }

  /** GET a JSON resource. */
  async getJson<T>(path: string, params?: QueryParams, signal?: AbortSignal): Promise<T> {
    const response = await this.request(`${path}${toQueryString(params)}`, { signal });
    return (await response.json()) as T;
  }

  /** GET a file (CSV export, document download). */
  async getBlob(
    path: string,
    params?: QueryParams,
    fallbackName = 'download',
  ): Promise<FileDownload> {
    const response = await this.request(`${path}${toQueryString(params)}`, {
      headers: { Accept: '*/*' },
    });
    return {
      blob: await response.blob(),
      fileName: fileNameFrom(response.headers.get('Content-Disposition'), fallbackName),
      headers: response.headers,
    };
  }

  /** POST multipart form data (document upload) and read the JSON response. */
  async postForm<T>(path: string, body: FormData): Promise<T> {
    const response = await this.request(path, { method: 'POST', body });
    return (await response.json()) as T;
  }

  /** DELETE a resource. */
  async delete(path: string): Promise<void> {
    await this.request(path, { method: 'DELETE' });
  }

  private async toError(response: Response): Promise<ApiError> {
    if (response.status === 401) {
      this.options.onUnauthorized();
    }
    let body: unknown = null;
    try {
      body = await response.json();
    } catch {
      // Not JSON (for example an HTML error page); fall back to the default message.
    }
    const requestId = isApiErrorBody(body) ? body.requestId : response.headers.get('X-Request-ID');
    const message = isApiErrorBody(body) ? body.message : defaultMessage(response.status);
    return new ApiError(response.status, message, requestId);
  }
}
