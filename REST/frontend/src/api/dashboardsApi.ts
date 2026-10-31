import { api } from './client';
import type { Dashboard, DashboardRequest } from './types';

/**
 * The signed-in user's dashboards. The user is sent as X-User-ID (like change-password) until the server has real
 * sessions (roadmap #4). Failures are thrown as ApiError with a {success:false, message} body (400 / 404 / 409).
 */
export const dashboardsApi = {
  list: (userId: string, signal?: AbortSignal) =>
    api.get<Dashboard[]>('/api/dashboards', { headers: { 'X-User-ID': userId }, signal }),

  create: (userId: string, dashboard: DashboardRequest) =>
    api.post<Dashboard>('/api/dashboards', dashboard, { headers: { 'X-User-ID': userId } }),

  update: (userId: string, dashboardPk: number, dashboard: DashboardRequest) =>
    api.put<Dashboard>(`/api/dashboards/${encodeURIComponent(dashboardPk)}`, dashboard, {
      headers: { 'X-User-ID': userId },
    }),

  remove: (userId: string, dashboardPk: number) =>
    api.delete<void>(`/api/dashboards/${encodeURIComponent(dashboardPk)}`, { headers: { 'X-User-ID': userId } }),
};
