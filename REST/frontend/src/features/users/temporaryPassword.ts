export const MIN_PASSWORD_LENGTH = 8;

/** Choice made in <TemporaryPasswordFields>. */
export interface TemporaryPasswordChoice {
  /** 'default' = the enterprise's default temporary password; 'custom' = typed by the admin. */
  mode: 'default' | 'custom';
  password: string;
  confirm: string;
}

export const DEFAULT_TEMPORARY_PASSWORD: TemporaryPasswordChoice = { mode: 'default', password: '', confirm: '' };

/** Validation message for the choice, or null when valid. */
export function validateTemporaryPassword(choice: TemporaryPasswordChoice): string | null {
  if (choice.mode === 'default') return null;
  if (choice.password.length < MIN_PASSWORD_LENGTH) {
    return `Temporary password must be at least ${MIN_PASSWORD_LENGTH} characters long.`;
  }
  if (choice.password !== choice.confirm) return 'Temporary password and confirmation do not match.';
  return null;
}

/** Value sent to the API: null means "use the enterprise default". */
export function temporaryPasswordValue(choice: TemporaryPasswordChoice): string | null {
  return choice.mode === 'custom' ? choice.password : null;
}
