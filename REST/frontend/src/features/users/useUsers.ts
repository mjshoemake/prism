import { useCallback, useEffect, useState } from 'react';
import { errorMessage } from '../../api/client';
import { usersApi } from '../../api/usersApi';
import type { CreateUserInput, User } from '../../api/types';
import { useToast } from '../../components/toast/useToast';

/** Editable fields of a row in the users table. */
export type UserEdits = Partial<Pick<User, 'email' | 'username' | 'is_active'>>;

/**
 * Loads the user list and wraps the user CRUD calls with success/failure toasts.
 * Each mutating action resolves to true on success.
 */
export function useUsers() {
  const { showToast } = useToast();
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const reload = useCallback(async () => {
    try {
      setLoading(true);
      setUsers(await usersApi.list());
      setError(null);
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  /** Local (unsaved) edit of a row; persisted by saveUser. */
  const editUser = useCallback((userId: string, edits: UserEdits) => {
    setUsers((prev) => prev.map((u) => (u.user_id === userId ? { ...u, ...edits } : u)));
  }, []);

  const run = useCallback(
    async (action: () => Promise<unknown>, success: string, failurePrefix: string, reloadAfter = true) => {
      try {
        await action();
        if (reloadAfter) await reload();
        showToast(success, 'success');
        return true;
      } catch (e) {
        showToast(`${failurePrefix}: ${errorMessage(e)}`, 'warning');
        return false;
      }
    },
    [reload, showToast],
  );

  const createUser = useCallback(
    (input: CreateUserInput) => run(() => usersApi.create(input), 'User created successfully', 'Create failed'),
    [run],
  );

  const saveUser = useCallback(
    (user: User) =>
      run(
        () => usersApi.update(user.user_id, { email: user.email, username: user.username, isActive: user.is_active }),
        'User updated successfully',
        'Update failed',
      ),
    [run],
  );

  const deleteUser = useCallback(
    (user: User) => run(() => usersApi.remove(user.user_id), 'User deleted successfully', 'Delete failed'),
    [run],
  );

  const setPassword = useCallback(
    (user: User, password: string) =>
      run(
        () => usersApi.setPassword(user.user_id, password),
        'Password updated successfully',
        'Password update failed',
        false,
      ),
    [run],
  );

  return { users, loading, error, reload, editUser, createUser, saveUser, deleteUser, setPassword };
}
