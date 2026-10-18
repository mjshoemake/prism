import { api } from './client';
import type { CreateUserInput, UpdateUserInput, User } from './types';

const usersPath = (userId: string) => `/api/users/${encodeURIComponent(userId)}`;

export const usersApi = {
  list: () => api.get<User[]>('/api/users'),

  create: (input: CreateUserInput) => api.post<User>('/api/users', input),

  update: (userId: string, input: UpdateUserInput) => api.put<void>(usersPath(userId), input),

  remove: (userId: string) => api.delete<void>(usersPath(userId)),

  setPassword: (userId: string, password: string) =>
    api.put<void>(`${usersPath(userId)}/password`, { password }),
};
