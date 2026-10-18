import type { AuthResult } from '../api/types';

const STORAGE_KEY = 'prism_auth';

export function loadSession(): AuthResult | null {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null') as AuthResult | null;
  } catch {
    return null;
  }
}

export function saveSession(session: AuthResult): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  } catch {
    // Storage unavailable (private mode, quota): the session just won't survive a reload.
  }
}

export function clearSession(): void {
  try {
    localStorage.removeItem(STORAGE_KEY);
  } catch {
    // ignore
  }
}
