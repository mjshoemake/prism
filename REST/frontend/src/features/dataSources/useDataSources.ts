import { useCallback, useEffect, useState } from 'react';
import { isAbortError, serverMessage } from '../../api/client';
import { dataSourcesApi } from '../../api/dataSourcesApi';
import type { DataSource } from '../../api/types';

/** The enterprise's data sources by name, with reload and delete. Create / update happen on the edit page. */
export function useDataSources(enterprisePk: number | null) {
  const [dataSources, setDataSources] = useState<DataSource[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [version, setVersion] = useState(0);

  useEffect(() => {
    if (enterprisePk === null) return;
    const controller = new AbortController();
    setLoading(true);
    dataSourcesApi
      .list(enterprisePk, controller.signal)
      .then((list) => {
        setDataSources(list);
        setError(null);
      })
      .catch((e) => {
        if (!isAbortError(e)) setError(`Failed to fetch data sources: ${serverMessage(e)}`);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [enterprisePk, version]);

  const reload = useCallback(() => setVersion((v) => v + 1), []);

  /** Throws the server's refusal (e.g. 409: facts or dimensions still belong to it). */
  const remove = useCallback(
    async (pk: number) => {
      if (enterprisePk === null) throw new Error('Your session has no enterprise.');
      await dataSourcesApi.remove(enterprisePk, pk);
      setDataSources((list) => list.filter((d) => d.data_source_pk !== pk));
    },
    [enterprisePk],
  );

  return { dataSources, loading, error, reload, remove };
}
