import { api } from './client';
import type { ConnectionTestResult, DataSource, DataSourceOptions, DataSourceRequest } from './types';

/**
 * Metrics → Data Sources, for one enterprise. TODO (roadmap #4): the server should take the enterprise from the
 * session. Failures are thrown as ApiError with a {success:false, message} body (400 / 404 / 409).
 */
const base = (enterprisePk: number, pk?: number, suffix = '') =>
  `/api/data-sources${pk === undefined ? '' : `/${encodeURIComponent(pk)}`}${suffix}?enterprisePk=${encodeURIComponent(enterprisePk)}`;

export const dataSourcesApi = {
  /** Whether this server allows the development-only connection, and the settings a data source may hold. */
  options: (signal?: AbortSignal) => api.get<DataSourceOptions>('/api/data-sources/options', { signal }),

  list: (enterprisePk: number, signal?: AbortSignal) => api.get<DataSource[]>(base(enterprisePk), { signal }),

  get: (enterprisePk: number, pk: number, signal?: AbortSignal) =>
    api.get<DataSource>(base(enterprisePk, pk), { signal }),

  create: (enterprisePk: number, request: DataSourceRequest) => api.post<DataSource>(base(enterprisePk), request),

  update: (enterprisePk: number, pk: number, request: DataSourceRequest) =>
    api.put<DataSource>(base(enterprisePk, pk), request),

  remove: (enterprisePk: number, pk: number) => api.delete<void>(base(enterprisePk, pk)),

  /** Checks that Prism can read the source's registered tables (the Prism database and direct_dev so far). */
  test: (enterprisePk: number, pk: number) =>
    api.post<ConnectionTestResult>(base(enterprisePk, pk, '/test'), undefined),
};
