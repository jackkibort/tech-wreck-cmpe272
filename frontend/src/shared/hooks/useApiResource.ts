import { useEffect, useRef, useState } from 'react';

import type { ApiClient } from '../api/client';
import { useApiClient } from '../api/apiClientContext';

/** Loads data with the session's API client; the signal is aborted when the key changes. */
export type Loader<T> = (client: ApiClient, signal: AbortSignal) => Promise<T>;

export interface Resource<T> {
  data: T | undefined;
  error: unknown;
  loading: boolean;
  /** Loads the same key again (for the "Try again" button). */
  reload: () => void;
}

interface Settled<T> {
  key: string;
  attempt: number;
  data?: T;
  error?: unknown;
}

/**
 * Loads an API resource identified by `key` and reloads it whenever the key changes.
 * Pass `null` as the key to skip loading (for example when the caller may not see the data).
 * Responses for an outdated key are discarded, so fast typing never shows stale results.
 */
export function useApiResource<T>(key: string | null, load: Loader<T>): Resource<T> {
  const client = useApiClient();
  const loadRef = useRef(load);
  const [attempt, setAttempt] = useState(0);
  const [settled, setSettled] = useState<Settled<T> | null>(null);

  useEffect(() => {
    loadRef.current = load;
  });

  useEffect(() => {
    if (key === null) {
      return;
    }
    const controller = new AbortController();
    loadRef.current(client, controller.signal).then(
      (data) => {
        if (!controller.signal.aborted) {
          setSettled({ key, attempt, data });
        }
      },
      (error: unknown) => {
        if (!controller.signal.aborted) {
          setSettled({ key, attempt, error });
        }
      },
    );
    return () => {
      controller.abort();
    };
  }, [client, key, attempt]);

  const current = settled !== null && settled.key === key && settled.attempt === attempt;
  return {
    data: current ? settled.data : undefined,
    error: current ? settled.error : undefined,
    loading: key !== null && !current,
    reload: () => {
      setAttempt((value) => value + 1);
    },
  };
}
