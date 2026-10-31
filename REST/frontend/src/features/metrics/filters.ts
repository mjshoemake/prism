import type { ChartFilter } from '../../api/types';

/** A clicked bar: its key (sent as the filter value) and its label (shown). */
export interface ChartBar {
  key: string | null;
  label: string | null;
}

/** A drill-down filter: "<focus level> = <bar>", added by clicking a bar. */
export interface Filter {
  focus_level_pk: number;
  /** Display name of the focus level (e.g. "Team"). */
  name: string;
  /** The clicked bar's key: what the server filters on. Null for bars whose key is empty. */
  value: string | null;
  /** The clicked bar's label: what the filter shows (e.g. "Dec 2024" for key "2024 12"). */
  label: string | null;
}

/** Shown for empty focus-level values (bars and filters). */
export const BLANK_LABEL = '(blank)';

/** Display text of a bar label or filter value. */
export function formatLabel(value: string | number | null): string {
  return value === null || value === '' ? BLANK_LABEL : String(value);
}

/** The filters as sent to the API: IDs and values only. */
export function toRequestFilters(filters: readonly Filter[]): ChartFilter[] {
  return filters.map((f) => ({ focus_level_pk: f.focus_level_pk, operator: 'eq', value: f.value }));
}

export function hasFilter(filters: readonly Filter[], focusLevelPk: number, value: string | null): boolean {
  return filters.some((f) => f.focus_level_pk === focusLevelPk && f.value === value);
}
