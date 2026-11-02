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
  /** The level a bar click drills down to (same focus area); null = no drill-down: the click only adds the filter. */
  drill_to_focus_level_pk: number | null;
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

/** A drill-down filter in a chart request: "focus level = key" (a ChartRow's key; null matches empty values). */
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

/**
 * One bar: its key, its label and the metric's value (null when it can't be calculated, e.g. 0 ÷ 0).
 * The key identifies the bar (send it back as a filter value); the label is only what the bar shows, and may repeat
 * (e.g. "Sprint 1" of two years). For a level without a separate key column, key and label are the same value.
 */
export interface ChartRow {
  key: string | null;
  label: string | number | null;
  value: number | null;
}

// ---- Dashboards -------------------------------------------------------------

/** Chart areas on a dashboard: 1 = one chart, 2 = two stacked, 4 = quadrants. */
export type DashboardLayout = 1 | 2 | 4;

/**
 * Y-axis scale of a chart area (SQL/021):
 * - metric: the metric's own default (`MetricInfo.scale_mode`);
 * - zero_100: 0 to 100, extended above 100 when a value exceeds it;
 * - fit: 0 to just above the highest value;
 * - custom: a hard minimum and/or maximum (`y_axis_min`, `y_axis_max`); bars beyond them are marked, not hidden.
 */
export type YAxisMode = 'metric' | 'zero_100' | 'fit' | 'custom';

/** The metric in one chart area and its y-axis scale; positions 1–4 in reading order (2-chart layout: 1 top, 2 bottom). */
export interface DashboardChart {
  position: number;
  metric_pk: number;
  y_axis_mode: YAxisMode;
  /** Custom scale only (null otherwise): the axis minimum; null = 0. */
  y_axis_min: number | null;
  /** Custom scale only (null otherwise): the axis maximum; null = fit to the data. */
  y_axis_max: number | null;
}

/**
 * A saved dashboard (GET /api/dashboards). `charts` may hold positions beyond the layout: they are kept (and come
 * back when switching to more charts) but aren't shown.
 */
export interface Dashboard {
  dashboard_pk: number;
  name: string;
  layout: DashboardLayout;
  charts: DashboardChart[];
}

/** Body of POST /api/dashboards and PUT /api/dashboards/{pk}: the whole dashboard. */
export interface DashboardRequest {
  name: string;
  layout: DashboardLayout;
  charts: DashboardChart[];
}

// ---- Data sources -------------------------------------------------------------

/**
 * How Prism reaches a database data source (SQL/013); prism_database = the demo STAR schema in Prism's database.
 * direct_dev is TEMPORARY and development-only (SQL/024): its login is saved in the data source's properties.
 */
export type DbConnectionMethod =
  | 'prism_database'
  | 'firewall_allow_list'
  | 'private_link'
  | 'outbound_agent'
  | 'direct_dev';

/** One saved setting of a data source (SQL/024). A secret (the password) comes back with value null. */
export interface DataSourceProperty {
  name: string;
  value: string | null;
  secret: boolean;
  has_value: boolean;
}

/**
 * A data source (GET /api/data-sources): where a STAR schema lives and how Prism connects. Exactly one connection:
 * a database (db_*) or a client-run API (api_*). Credentials are never returned: *_credentials_ref is the NAME of the
 * secret in the secrets store; the temporary direct_dev method keeps its login in properties (password value hidden).
 */
export interface DataSource {
  data_source_pk: number;
  enterprise_pk: number;
  name: string;
  description: string | null;
  connection_type: 'database' | 'api';
  db_connection_method: DbConnectionMethod | null;
  db_host: string | null;
  db_port: number | null;
  db_name: string | null;
  db_credentials_ref: string | null;
  api_url: string | null;
  api_credentials_ref: string | null;
  fact_count: number;
  dimension_count: number;
  metric_count: number;
  created_at: Timestamp | null;
  updated_at: Timestamp | null;
  properties: DataSourceProperty[];
}

/** Body of POST /api/data-sources and PUT /api/data-sources/{pk}; fields that don't apply are ignored. */
export type DataSourceRequest = Pick<
  DataSource,
  | 'name'
  | 'description'
  | 'connection_type'
  | 'db_connection_method'
  | 'db_host'
  | 'db_port'
  | 'db_name'
  | 'db_credentials_ref'
  | 'api_url'
  | 'api_credentials_ref'
> & {
  /** Replaces the saved settings (direct_dev only). A secret with value null keeps the saved one. */
  properties?: { name: string; value: string | null }[];
};

/** GET /api/data-sources/options: whether the development-only method is on, and the settings it may hold. */
export interface DataSourceOptions {
  dev_connections_allowed: boolean;
  properties: { name: string; label: string; secret: boolean; required: boolean; driver: boolean; hint: string }[];
}

/**
 * Result of POST /api/data-sources/{pk}/test: the tables the connection's login can read, per schema, and one line per
 * registered fact / dimension table.
 */
export interface ConnectionTestResult {
  success: boolean;
  message: string;
  readable: { schema: string; tables: number }[];
  tables: { kind: 'fact' | 'dimension'; name: string; table: string; readable: boolean; error: string | null }[];
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
