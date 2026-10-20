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

/** Timestamps arrive as ISO-8601 strings (Jackson's Spring Boot default); epoch millis are tolerated. */
export type Timestamp = string | number;

export interface User {
  user_id: string;
  enterprise_pk: number;
  email: string;
  username: string | null;
  is_active: boolean;
  /** True until the user replaces a temporary password; the UI forces a password change. */
  is_temp_password: boolean;
  failed_login_count: number;
  locked_until: Timestamp | null;
  last_login_at: Timestamp | null;
  created_at: Timestamp;
  updated_at: Timestamp;
}

export interface CreateUserInput {
  enterprise_pk: number;
  email: string;
  username: string;
  /** null = use the enterprise's default temporary password. */
  temporaryPassword: string | null;
}

export interface UpdateUserInput {
  email: string;
  username: string | null;
  isActive: boolean;
}

// ---- Auth -----------------------------------------------------------------

export type AuthenticatedUser = Omit<User, 'created_at' | 'updated_at'>;

/**
 * Response of /api/auth/login, /api/auth/change-password, and failures of the user-admin endpoints
 * (UserActionResult on the server).
 */
export interface AuthResult {
  success: boolean;
  message?: string;
  user?: AuthenticatedUser;
}
