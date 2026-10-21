import { api, withActionResult } from './client';
import type { CreateUserInput, TemporaryPasswordResult, UpdateUserInput, User } from './types';

const usersPath = (userId: string) => `/api/users/${encodeURIComponent(userId)}`;

export const usersApi = {
  /** Users of one enterprise. TODO(backlog #5): the server should take the enterprise from the session. */
  list: (enterprisePk: number, signal?: AbortSignal) =>
    api.get<User[]>(`/api/users?enterprisePk=${encodeURIComponent(enterprisePk)}`, { signal }),

  /** Creates a user with a generated temporary password. Rejections (duplicate email…) resolve to {success:false}. */
  create: (input: CreateUserInput) =>
    withActionResult<TemporaryPasswordResult>(() => api.post<TemporaryPasswordResult>('/api/users', input)),

  update: (userId: string, input: UpdateUserInput) => api.put<void>(usersPath(userId), input),

  /** Resets to a generated temporary password; the user must change it at next login. */
  resetPassword: (userId: string) =>
    withActionResult<TemporaryPasswordResult>(() =>
      api.put<TemporaryPasswordResult>(`${usersPath(userId)}/reset-password`, undefined),
    ),
};
