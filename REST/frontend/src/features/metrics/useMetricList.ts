import { useEffect, useState } from 'react';
import { errorMessage, isAbortError } from '../../api/client';
import { metricsApi } from '../../api/metricsApi';
import type { MetricInfo } from '../../api/types';

/** The metrics to choose from (from the Metrics table, via the API) and the selected one (the first by default). */
export function useMetricList() {
  const [metrics, setMetrics] = useState<MetricInfo[]>([]);
  const [selectedMetric, setSelectedMetric] = useState<MetricInfo | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const controller = new AbortController();
    metricsApi
      .getMetrics(controller.signal)
      .then((list) => {
        setMetrics(list);
        setSelectedMetric(list[0] ?? null);
      })
      .catch((e) => {
        if (!isAbortError(e)) setError(`Failed to fetch metrics: ${errorMessage(e)}`);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, []);

  return { metrics, selectedMetric, selectMetric: setSelectedMetric, loading, error };
}
