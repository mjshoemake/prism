/**
 * Shapes of the JSON returned by the Prism REST API.
 * Field names are the database column names, because the API returns rows as-is.
 */

// ---- Metrics --------------------------------------------------------------

export interface FocusArea {
  focus_area_pk: number;
  name: string;
}

export interface FocusLevel {
  focus_area_pk: number;
  name: string;
  /** Column of the STAR schema this level groups by (e.g. "year_month"). */
  column_name: string;
}

/** Body of POST /api/metrics/sprint-metrics. */
export interface SprintMetricsRequest {
  focusLevelColumn: string;
  valueColumnsForSelectClause: string;
  whereClause: string;
}

/** One result row: the focus-level column plus one column per metric (keyed by SQL alias). */
export type MetricRow = Record<string, string | number | null>;

// ---- Users ----------------------------------------------------------------

export interface User {
  user_id: string;
  enterprise_pk: number;
  email: string;
  username: string | null;
  is_active: boolean;
  is_temp_password: boolean;
  failed_login_count: number;
  locked_until: string | null;
  last_login_at: string | null;
  created_at: string;
  updated_at: string;
}

export interface CreateUserInput {
  enterprise_pk: number;
  email: string;
  username: string;
}

export interface UpdateUserInput {
  email: string;
  username: string | null;
  isActive: boolean;
}

// ---- Auth -----------------------------------------------------------------

export type AuthenticatedUser = Omit<User, 'created_at' | 'updated_at'>;

/** Response of /api/auth/login and /api/auth/change-password (UserActionResult on the server). */
export interface AuthResult {
  success: boolean;
  message?: string;
  user?: AuthenticatedUser;
}
