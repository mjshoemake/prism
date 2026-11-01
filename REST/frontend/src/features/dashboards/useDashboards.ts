import { useCallback, useEffect, useState } from 'react';
import { ApiError, errorMessage, isAbortError } from '../../api/client';
import { dashboardsApi } from '../../api/dashboardsApi';
import type { Dashboard, DashboardRequest } from '../../api/types';

/**
 * The signed-in user's saved dashboards, by name, and the calls that change them. Nothing is saved implicitly: the
 * edit page saves a whole dashboard at once. Calls throw on failure; `serverMessage` turns the error into the
 * server's text (e.g. "You already have a dashboard named 'X'.").
 */
export function useDashboards(userId: string | null) {
  const [dashboards, setDashboards] = useState<Dashboard[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    if (!userId) return;
    const controller = new AbortController();
    setLoading(true);
    dashboardsApi
      .list(userId, controller.signal)
      .then((list) => {
        setDashboards(list);
        setError(null);
      })
      .catch((e) => {
        if (!isAbortError(e)) setError(`Failed to fetch dashboards: ${serverMessage(e)}`);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [userId, version]);

  /** Fetches the list again (e.g. after another tab changed it). */
  const reload = useCallback(() => setVersion((v) => v + 1), []);

  const create = useCallback(
    async (request: DashboardRequest): Promise<Dashboard> => {
      if (!userId) throw new Error('Not signed in.');
      const created = await dashboardsApi.create(userId, request);
      setDashboards((list) => sortByName([...list, created]));
      return created;
    },
    [userId],
  );

  const update = useCallback(
    async (dashboardPk: number, request: DashboardRequest): Promise<Dashboard> => {
      if (!userId) throw new Error('Not signed in.');
      const saved = await dashboardsApi.update(userId, dashboardPk, request);
      setDashboards((list) => sortByName(list.map((d) => (d.dashboard_pk === saved.dashboard_pk ? saved : d))));
      return saved;
    },
    [userId],
  );

  const remove = useCallback(
    async (dashboardPk: number): Promise<void> => {
      if (!userId) throw new Error('Not signed in.');
      await dashboardsApi.remove(userId, dashboardPk);
      setDashboards((list) => list.filter((d) => d.dashboard_pk !== dashboardPk));
    },
    [userId],
  );

  return { dashboards, loading, error, reload, create, update, remove };
}

export function sortByName(list: Dashboard[]): Dashboard[] {
  return [...list].sort((a, b) => a.name.localeCompare(b.name) || a.dashboard_pk - b.dashboard_pk);
}

/** The server's {success:false, message} text, else the general error text. */
export function serverMessage(error: unknown): string {
  if (error instanceof ApiError) {
    const body = error.json<{ message?: string }>();
    if (body?.message) return body.message;
  }
  return errorMessage(error);
}

/** True for a 409 (a dashboard with that name already exists). */
export function isConflict(error: unknown): boolean {
  return error instanceof ApiError && error.status === 409;
}
