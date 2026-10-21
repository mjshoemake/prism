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
  focus_level_pk: number;
  focus_area_pk: number;
  name: string;
  /** Column of the STAR schema this level groups by (e.g. "year_month"). Informational: requests use the pk. */
  column_name: string;
}

/**
 * A metric as the API describes it (GET /api/metrics/list). Display information only: how it is calculated stays
 * on the server, in the Metrics table.
 */
export interface MetricInfo {
  metric_pk: number;
  name: string;
  metric_type: 'percentage' | 'ratio' | 'average' | 'sum';
  title: string;
  /** e.g. "%" or "points"; null for none. */
  unit: string | null;
  decimal_places: number;
  /** Y-axis default: 0–100 (extends above 100 when needed), fitted to the data, or fixed bounds. */
  scale_mode: 'zero_to_100' | 'fit' | 'fixed';
  scale_min: number | null;
  scale_max: number | null;
}

/** A drill-down filter in a chart request: "focus level = value" (null matches empty values). */
export interface ChartFilter {
  focus_level_pk: number;
  operator: 'eq';
  value: string | null;
}

/** Body of POST /api/metrics/data: IDs and values only, never SQL. */
export interface ChartDataRequest {
  metric_pk: number;
  focus_level_pk: number;
  filters: ChartFilter[];
}

/** One bar: the focus-level value and the metric's value (null when it can't be calculated, e.g. 0 ÷ 0). */
export interface ChartRow {
  label: string | number | null;
  value: number | null;
}

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
  /** When the temporary password stops working; null once the user has chosen their own password. */
  temp_password_expires_at: Timestamp | null;
  failed_login_count: number;
  locked_until: Timestamp | null;
  last_login_at: Timestamp | null;
  created_at: Timestamp;
  updated_at: Timestamp;
}

export interface CreateUserInput {
  enterprise_pk: number;
  email: string;
  /** null when not given (stored as NULL; the UI shows the email instead). */
  username: string | null;
}

/**
 * Response of creating a user or resetting a password: the user plus the generated temporary password,
 * in clear text, shown once to the administrator. On failure: {success:false, message}.
 */
export interface TemporaryPasswordResult {
  success: boolean;
  message?: string;
  user?: User;
  temporaryPassword?: string;
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

// ---- System settings ------------------------------------------------------

/** Enterprise settings (GET /api/settings). */
export interface SystemSettings {
  enterprise_pk: number;
  /** Hours a generated temporary password can be used to sign in. */
  temp_password_valid_hours: number;
  updated_at: Timestamp | null;
}
