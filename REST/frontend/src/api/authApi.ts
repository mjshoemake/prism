import { api, withActionResult } from './client';
import type { AuthResult } from './types';

// The auth endpoints answer failures (400/401) with {"success": false, "message": "..."}; withActionResult
// returns that body instead of throwing.
const withAuthResult = withActionResult<AuthResult>;

export const authApi = {
  login: (email: string, password: string) =>
    withAuthResult(() => api.post<AuthResult>('/api/auth/login', { email, password })),

  changePassword: (userId: string, currentPassword: string, newPassword: string) =>
    withAuthResult(() =>
      api.post<AuthResult>(
        '/api/auth/change-password',
        { currentPassword, newPassword },
        { headers: { 'X-User-ID': userId } },
      ),
    ),
};
