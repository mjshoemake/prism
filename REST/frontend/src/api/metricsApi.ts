import { api } from './client';
import type { FocusArea, FocusLevel, MetricRow, SprintMetricsRequest } from './types';

export const metricsApi = {
  getFocusAreas: (signal?: AbortSignal) =>
    api.get<FocusArea[]>('/api/metrics/focus-areas-list', { signal }),

  getFocusLevels: (focusAreaPk: number, signal?: AbortSignal) =>
    api.get<FocusLevel[]>(`/api/metrics/focus-levels-list/${encodeURIComponent(focusAreaPk)}`, { signal }),

  getSprintMetrics: (request: SprintMetricsRequest, signal?: AbortSignal) =>
    api.post<MetricRow[]>('/api/metrics/sprint-metrics', request, { signal }),
};
