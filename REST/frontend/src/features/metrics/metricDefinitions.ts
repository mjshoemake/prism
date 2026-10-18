/**
 * Metrics the dashboard can chart.
 *
 * NOTE: `selectSql` is a raw SQL fragment sent to the API, because /api/metrics/sprint-metrics currently
 * accepts SQL from the client (backlog item: move metric definitions server-side).
 */
export interface MetricDefinition {
  /** Column alias the API returns the value under. */
  key: string;
  /** Human-readable name used in headings. */
  title: string;
  /** Dataset label shown in the chart legend. */
  legendLabel: string;
  /** Upper bound of the y-axis, or undefined to auto-scale. */
  max?: number;
  /** Formats y-axis tick values. */
  formatTick: (value: number | string) => string;
  /** SQL appended to the SELECT list. */
  selectSql: string;
}

export const SAY_DO_RATIO: MetricDefinition = {
  key: 'Say/Do Ratio',
  title: 'Say/Do Ratio',
  legendLabel: 'Say/Do Ratio (%)',
  max: 100,
  formatTick: (value) => `${value}%`,
  selectSql:
    'ROUND((SUM(smf.committed_story_points_completed)::DECIMAL / SUM(smf.committed_story_points)::DECIMAL) * 100, 2) as "Say/Do Ratio"',
};
