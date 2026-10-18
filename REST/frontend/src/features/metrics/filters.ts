/** A drill-down filter: "<focus level> = <value>", added by clicking a bar. */
export interface Filter {
  /** Display name of the focus level (e.g. "Team"). */
  name: string;
  /** Column the filter applies to. */
  column_name: string;
  value: string;
}

/**
 * Builds the WHERE clause sent to the API from the active filters
 * (single quotes in values are escaped by doubling them).
 */
export function buildWhereClause(filters: readonly Filter[]): string {
  if (filters.length === 0) {
    return '';
  }
  const conditions = filters.map((f) => `${f.column_name} = '${f.value.replace(/'/g, "''")}'`);
  return `WHERE ${conditions.join(' and ')}`;
}

export function hasFilter(filters: readonly Filter[], columnName: string, value: string): boolean {
  return filters.some((f) => f.column_name === columnName && f.value === value);
}
