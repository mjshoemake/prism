import type { Timestamp } from '../../api/types';

const DATE_TIME = new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' });

/** "Oct 20, 2026, 2:24 PM" in the browser's locale, or an em dash when empty/invalid. */
export function formatDateTime(value: Timestamp | null | undefined): string {
  if (value === null || value === undefined || value === '') return '—';
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? '—' : DATE_TIME.format(date);
}

/** Epoch millis for sorting; empty values sort first. */
export function timestampValue(value: Timestamp | null | undefined): number {
  if (value === null || value === undefined || value === '') return 0;
  const time = new Date(value).getTime();
  return Number.isNaN(time) ? 0 : time;
}
