import type { ReactNode } from 'react';

import { ApiClientContext } from './apiClientContext';
import type { ApiClient } from './client';

/** Makes the session's API client available to every page. */
export function ApiClientProvider({
  client,
  children,
}: {
  client: ApiClient;
  children: ReactNode;
}) {
  return <ApiClientContext.Provider value={client}>{children}</ApiClientContext.Provider>;
}
