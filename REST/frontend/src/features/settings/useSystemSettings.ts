import { useCallback, useEffect, useState } from 'react';
import { errorMessage } from '../../api/client';
import { settingsApi } from '../../api/settingsApi';
import type { SystemSettings } from '../../api/types';
import { useToast } from '../../components/toast/useToast';

export interface SaveOutcome {
  ok: boolean;
  message?: string;
}

/** System settings of one enterprise, plus saving them. */
export function useSystemSettings(enterprisePk: number | null) {
  const { showToast } = useToast();
  const [settings, setSettings] = useState<SystemSettings | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(
    async (signal?: AbortSignal) => {
      if (enterprisePk === null) return;
      try {
        setLoading(true);
        setSettings(await settingsApi.get(enterprisePk, signal));
        setError(null);
      } catch (e) {
        if (!signal?.aborted) setError(`Failed to load system settings: ${errorMessage(e)}`);
      } finally {
        if (!signal?.aborted) setLoading(false);
      }
    },
    [enterprisePk],
  );

  useEffect(() => {
    const controller = new AbortController();
    void reload(controller.signal);
    return () => controller.abort();
  }, [reload]);

  const save = useCallback(
    async (tempPasswordValidHours: number): Promise<SaveOutcome> => {
      if (enterprisePk === null) return { ok: false, message: 'Your session has no enterprise. Sign in again.' };
      try {
        const result = await settingsApi.save(enterprisePk, tempPasswordValidHours);
        if (!result.success) return { ok: false, message: result.message ?? 'The settings could not be saved.' };
        await reload();
        showToast(result.message ?? 'Settings saved', 'success');
        return { ok: true };
      } catch (e) {
        return { ok: false, message: `Save failed: ${errorMessage(e)}` };
      }
    },
    [enterprisePk, reload, showToast],
  );

  return { settings, loading, error, save };
}
