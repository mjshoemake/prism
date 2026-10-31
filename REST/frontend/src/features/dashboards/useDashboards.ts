import { useCallback, useEffect, useState } from 'react';
import { ApiError, errorMessage, isAbortError } from '../../api/client';
import { dashboardsApi } from '../../api/dashboardsApi';
import type { Dashboard, DashboardLayout } from '../../api/types';
import { useToast } from '../../components/toast/useToast';
import { withMetric } from './layouts';

/**
 * The signed-in user's saved dashboards and the one being viewed. Every change (layout, a chart area's metric, the
 * name) is saved right away; if saving fails, the change is undone and the server's message shown.
 */
export function useDashboards(userId: string | null, defaultMetricPk: number | null) {
  const { showToast } = useToast();
  const [dashboards, setDashboards] = useState<Dashboard[]>([]);
  const [selectedPk, setSelectedPk] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!userId) return;
    const controller = new AbortController();
    setLoading(true);
    dashboardsApi
      .list(userId, controller.signal)
      .then((list) => {
        setDashboards(list);
        setSelectedPk((current) => (list.some((d) => d.dashboard_pk === current) ? current : (list[0]?.dashboard_pk ?? null)));
      })
      .catch((e) => {
        if (!isAbortError(e)) setError(`Failed to fetch dashboards: ${serverMessage(e)}`);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [userId]);

  const selected = dashboards.find((d) => d.dashboard_pk === selectedPk) ?? null;

  /** Saves the whole dashboard; shows it at once and undoes the change if the server refuses it. */
  const save = useCallback(
    async (next: Dashboard): Promise<boolean> => {
      if (!userId) return false;
      const previous = dashboards;
      setDashboards((list) => list.map((d) => (d.dashboard_pk === next.dashboard_pk ? next : d)));
      try {
        const saved = await dashboardsApi.update(userId, next.dashboard_pk, {
          name: next.name,
          layout: next.layout,
          charts: next.charts,
        });
        setDashboards((list) => sortByName(list.map((d) => (d.dashboard_pk === saved.dashboard_pk ? saved : d))));
        return true;
      } catch (e) {
        setDashboards(previous);
        showToast(`Not saved: ${serverMessage(e)}`, 'warning');
        return false;
      }
    },
    [dashboards, showToast, userId],
  );

  const create = useCallback(
    async (name: string): Promise<boolean> => {
      if (!userId) return false;
      try {
        const created = await dashboardsApi.create(userId, {
          name,
          layout: 1,
          charts: defaultMetricPk === null ? [] : [{ position: 1, metric_pk: defaultMetricPk }],
        });
        setDashboards((list) => sortByName([...list, created]));
        setSelectedPk(created.dashboard_pk);
        showToast(`Created dashboard: ${created.name}`, 'success');
        return true;
      } catch (e) {
        showToast(`Not created: ${serverMessage(e)}`, 'warning');
        return false;
      }
    },
    [defaultMetricPk, showToast, userId],
  );

  const remove = useCallback(async () => {
    if (!userId || !selected) return;
    try {
      await dashboardsApi.remove(userId, selected.dashboard_pk);
      const rest = dashboards.filter((d) => d.dashboard_pk !== selected.dashboard_pk);
      setDashboards(rest);
      setSelectedPk(rest[0]?.dashboard_pk ?? null);
      showToast(`Deleted dashboard: ${selected.name}`, 'info');
    } catch (e) {
      showToast(`Not deleted: ${serverMessage(e)}`, 'warning');
    }
  }, [dashboards, selected, showToast, userId]);

  const setLayout = useCallback(
    (layout: DashboardLayout) => {
      if (selected && selected.layout !== layout) void save({ ...selected, layout });
    },
    [save, selected],
  );

  const setMetric = useCallback(
    (position: number, metricPk: number | null) => {
      if (selected) void save({ ...selected, charts: withMetric(selected.charts, position, metricPk) });
    },
    [save, selected],
  );

  const rename = useCallback(
    (name: string) => (selected ? save({ ...selected, name: name.trim() }) : Promise.resolve(false)),
    [save, selected],
  );

  return {
    dashboards,
    selected,
    select: setSelectedPk,
    loading,
    error,
    create,
    rename,
    remove,
    setLayout,
    setMetric,
  };
}

function sortByName(list: Dashboard[]): Dashboard[] {
  return [...list].sort((a, b) => a.name.localeCompare(b.name) || a.dashboard_pk - b.dashboard_pk);
}

function serverMessage(error: unknown): string {
  if (error instanceof ApiError) {
    const body = error.json<{ message?: string }>();
    if (body?.message) return body.message;
  }
  return errorMessage(error);
}
