import { api, ApiError } from './client';
import type { AuthResult } from './types';

/**
 * The auth endpoints answer failures (400/401) with a JSON AuthResult body, e.g.
 * {"success": false, "message": "Invalid email or password."}. Return that body instead of throwing.
 */
async function withAuthResult(call: () => Promise<AuthResult>): Promise<AuthResult> {
  try {
    return await call();
  } catch (error) {
    const result = error instanceof ApiError ? error.json<AuthResult>() : undefined;
    if (result && typeof result.success === 'boolean') {
      return result;
    }
    throw error;
  }
}

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
