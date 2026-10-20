import { useCallback, useEffect, useState } from 'react';
import { errorMessage } from '../../api/client';
import { usersApi } from '../../api/usersApi';
import type { UpdateUserInput, User } from '../../api/types';
import { useToast } from '../../components/toast/useToast';

/** Result of a user action, for showing a message inside the modal that started it. */
export interface ActionOutcome {
  ok: boolean;
  message?: string;
}

/**
 * Users of one enterprise plus the admin actions on them. Success shows a toast; failures are
 * returned as an ActionOutcome so the open modal can show the message next to the form.
 */
export function useUsers(enterprisePk: number | null) {
  const { showToast } = useToast();
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(
    async (signal?: AbortSignal) => {
      if (enterprisePk === null) return;
      try {
        setLoading(true);
        setUsers(await usersApi.list(enterprisePk, signal));
        setError(null);
      } catch (e) {
        if (!signal?.aborted) setError(`Failed to load users: ${errorMessage(e)}`);
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

  const createUser = useCallback(
    async (email: string, username: string, temporaryPassword: string | null): Promise<ActionOutcome> => {
      if (enterprisePk === null) return { ok: false, message: 'Your session has no enterprise. Sign in again.' };
      try {
        const result = await usersApi.create({ enterprise_pk: enterprisePk, email, username, temporaryPassword });
        if (!result.success) return { ok: false, message: result.message ?? 'The user could not be created.' };
        await reload();
        showToast(`User ${email} created`, 'success');
        return { ok: true };
      } catch (e) {
        return { ok: false, message: `Create failed: ${errorMessage(e)}` };
      }
    },
    [enterprisePk, reload, showToast],
  );

  const updateUser = useCallback(
    async (user: User, changes: UpdateUserInput): Promise<ActionOutcome> => {
      try {
        await usersApi.update(user.user_id, changes);
        await reload();
        showToast('User updated', 'success');
        return { ok: true };
      } catch (e) {
        return { ok: false, message: `Update failed: ${errorMessage(e)}` };
      }
    },
    [reload, showToast],
  );

  const resetPassword = useCallback(
    async (user: User, temporaryPassword: string | null): Promise<ActionOutcome> => {
      try {
        const result = await usersApi.resetPassword(user.user_id, temporaryPassword);
        if (!result.success) return { ok: false, message: result.message ?? 'The password could not be reset.' };
        await reload();
        showToast(result.message ?? 'Password reset', 'success');
        return { ok: true, message: result.message };
      } catch (e) {
        return { ok: false, message: `Reset failed: ${errorMessage(e)}` };
      }
    },
    [reload, showToast],
  );

  return { users, loading, error, reload, createUser, updateUser, resetPassword };
}
