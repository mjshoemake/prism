import { api, withActionResult } from './client';
import type { AuthResult, SystemSettings } from './types';

export const settingsApi = {
  /** Settings of one enterprise. TODO(backlog #5): the server should take the enterprise from the session. */
  get: (enterprisePk: number, signal?: AbortSignal) =>
    api.get<SystemSettings>(`/api/settings?enterprisePk=${encodeURIComponent(enterprisePk)}`, { signal }),

  /** Saves the enterprise's settings. Rejections resolve to {success:false,message}. */
  save: (enterprisePk: number, tempPasswordValidHours: number) =>
    withActionResult<AuthResult>(() =>
      api.put<AuthResult>('/api/settings', {
        enterprise_pk: enterprisePk,
        temp_password_valid_hours: tempPasswordValidHours,
      }),
    ),
};
