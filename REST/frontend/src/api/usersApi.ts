import { api, withActionResult } from './client';
import type { AuthResult, CreateUserInput, UpdateUserInput, User } from './types';

const usersPath = (userId: string) => `/api/users/${encodeURIComponent(userId)}`;

/** Outcome of creating a user: the new user, or a message explaining why it was rejected. */
export interface CreateUserResult {
  success: boolean;
  message?: string;
  user?: User;
}

export const usersApi = {
  /** Users of one enterprise. TODO(backlog #5): the server should take the enterprise from the session. */
  list: (enterprisePk: number, signal?: AbortSignal) =>
    api.get<User[]>(`/api/users?enterprisePk=${encodeURIComponent(enterprisePk)}`, { signal }),

  /** Rejections (missing default password, duplicate email…) resolve to {success:false,message}. */
  create: (input: CreateUserInput) =>
    withActionResult<CreateUserResult>(async () => ({
      success: true,
      user: await api.post<User>('/api/users', input),
    })),

  update: (userId: string, input: UpdateUserInput) => api.put<void>(usersPath(userId), input),

  /** Resets to a temporary password (null = enterprise default); the user must change it at next login. */
  resetPassword: (userId: string, temporaryPassword: string | null) =>
    withActionResult<AuthResult>(() =>
      api.put<AuthResult>(`${usersPath(userId)}/reset-password`, { temporaryPassword }),
    ),
};
