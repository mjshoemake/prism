import { api } from './client';
import type { ChartDataRequest, ChartRow, FocusArea, FocusLevel, MetricInfo } from './types';

export const metricsApi = {
  /** Metrics to choose from, in display order. */
  getMetrics: (signal?: AbortSignal) => api.get<MetricInfo[]>('/api/metrics/list', { signal }),

  getFocusAreas: (signal?: AbortSignal) =>
    api.get<FocusArea[]>('/api/metrics/focus-areas-list', { signal }),

  getFocusLevels: (focusAreaPk: number, signal?: AbortSignal) =>
    api.get<FocusLevel[]>(`/api/metrics/focus-levels-list/${encodeURIComponent(focusAreaPk)}`, { signal }),

  /** Chart rows for one metric, grouped by one focus level, filtered by the drill-down filters. */
  getChartData: (request: ChartDataRequest, signal?: AbortSignal) =>
    api.post<ChartRow[]>('/api/metrics/data', request, { signal }),
};
