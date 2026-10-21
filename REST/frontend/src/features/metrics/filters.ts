import type { ChartFilter } from '../../api/types';

/** A drill-down filter: "<focus level> = <value>", added by clicking a bar. */
export interface Filter {
  focus_level_pk: number;
  /** Display name of the focus level (e.g. "Team"). */
  name: string;
  /** The clicked bar's label; null for bars whose value is empty. */
  value: string | null;
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
