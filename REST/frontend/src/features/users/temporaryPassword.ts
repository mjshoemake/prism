import type { User } from '../../api/types';
import { timestampValue } from './format';

/** True when the user still has a temporary password and it has expired (they can't sign in). */
export function isTemporaryPasswordExpired(user: User, now = Date.now()): boolean {
  const expiresAt = timestampValue(user.temp_password_expires_at);
  return user.is_temp_password && expiresAt > 0 && expiresAt <= now;
}

/**
 * Copies text to the clipboard. The Clipboard API only exists on secure origins (https or localhost),
 * so on plain http the text in `fallbackInput` is selected and copied the old way.
 */
export async function copyToClipboard(text: string, fallbackInput?: HTMLInputElement | null): Promise<boolean> {
  if (window.isSecureContext && navigator.clipboard) {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch {
      // Permission denied: fall through to the selection-based copy.
    }
  }
  if (!fallbackInput) return false;
  fallbackInput.focus();
  fallbackInput.select();
  try {
    return document.execCommand('copy');
  } catch {
    return false;
  }
}
