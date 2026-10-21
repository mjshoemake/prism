/** Same range as the server (SystemSettingsService) and the database CHECK constraint (SQL/010). */
export const MIN_VALID_HOURS = 1;
export const MAX_VALID_HOURS = 720;

/** Message for an invalid "valid for (hours)" entry, or null when valid. */
export function validateValidHours(text: string): string | null {
  const hours = Number(text);
  if (text.trim() === '' || !Number.isInteger(hours) || hours < MIN_VALID_HOURS || hours > MAX_VALID_HOURS) {
    return `Enter a whole number of hours from ${MIN_VALID_HOURS} to ${MAX_VALID_HOURS} (30 days).`;
  }
  return null;
}
