import { createContext, useContext } from 'react';

import type { ApiClient } from './client';

export const ApiClientContext = createContext<ApiClient | null>(null);

/**
 * Returns the API client for the signed-in session.
 * @throws when used outside ApiClientProvider.
 */
export function useApiClient(): ApiClient {
  const client = useContext(ApiClientContext);
  if (!client) {
    throw new Error('useApiClient must be used inside ApiClientProvider');
  }
  return client;
}
