import type { DashboardChart, MetricInfo } from '../../api/types';

/** "83.27%", "11.21 points", or "—" when there is no value. */
export function formatValue(metric: MetricInfo, value: number | null): string {
  if (value === null || Number.isNaN(value)) return '—';
  const number = value.toLocaleString(undefined, {
    minimumFractionDigits: metric.decimal_places,
    maximumFractionDigits: metric.decimal_places,
  });
  if (!metric.unit) return number;
  return metric.unit === '%' ? `${number}%` : `${number} ${metric.unit}`;
}

/** Axis tick text: "%" is appended for percentages; other units appear in the legend instead. */
export function formatTick(metric: MetricInfo, value: number | string): string {
  return metric.unit === '%' ? `${value}%` : String(value);
}

/** Legend text: "Say/Do Ratio (%)". */
export function legendLabel(metric: MetricInfo): string {
  return metric.unit ? `${metric.title} (${metric.unit})` : metric.title;
}

/**
 * The y-axis of one chart, as Chart.js options plus whether its bounds are hard.
 * - Soft (`hard: false`): the suggested bounds give way to the data, so no bar is ever cut off.
 * - Hard (custom scales): `min` / `max` hold, and bars beyond them are drawn to the edge and marked (barber pole).
 */
export interface ChartScale {
  min?: number;
  max?: number;
  suggestedMin?: number;
  suggestedMax?: number;
  hard: boolean;
}

/** The chart area's scale settings; a chart with none (or mode "metric") uses the metric's own default. */
export type ScaleSetting = Pick<DashboardChart, 'y_axis_mode' | 'y_axis_min' | 'y_axis_max'>;

/**
 * Y-axis bounds of a chart (docs/OVERVIEW.md §4.3, SQL/021):
 * - zero_100: 0–100, extended to a round number above the highest value when a value exceeds 100 (owner, 2026-10-31);
 * - fit: from 0 to a round number above the highest value;
 * - custom: the chart's own minimum and/or maximum, hard;
 * - metric: the metric's default: zero_to_100 and fit as above; `fixed` as suggestions only (a metric default is a
 *   starting point, a chart's custom scale is the user's deliberate choice).
 */
export function effectiveScale(metric: MetricInfo, setting?: ScaleSetting | null): ChartScale {
  switch (setting?.y_axis_mode ?? 'metric') {
    case 'zero_100':
      return { min: 0, suggestedMax: 100, hard: false };
    case 'fit':
      return { hard: false };
    case 'custom':
      return {
        min: setting?.y_axis_min ?? undefined,
        max: setting?.y_axis_max ?? undefined,
        hard: true,
      };
    default:
      switch (metric.scale_mode) {
        case 'zero_to_100':
          return { min: 0, suggestedMax: 100, hard: false };
        case 'fixed':
          return { suggestedMin: metric.scale_min ?? undefined, suggestedMax: metric.scale_max ?? undefined, hard: false };
        default:
          return { hard: false };
      }
  }
}

/** Where a value sits against a hard scale: within it, above its maximum, or below its minimum. */
export type BarFit = 'within' | 'above' | 'below';

/** A value as drawn on a hard scale: kept inside the axis, with a note of which edge it was cut at. */
export interface ClippedValue {
  /** The height to draw; null = no bar (no value). */
  drawn: number | null;
  fit: BarFit;
}

/** Share of the axis range a bar below a raised minimum is drawn with, so it stays visible (and clickable). */
export const BELOW_MIN_STUB = 0.03;

/**
 * Keeps a value inside a hard scale: above the maximum → drawn at the maximum; below a minimum → a short stub just
 * above it. Soft scales draw every value as it is. Only what is drawn changes: tooltips show the real value.
 * `top` is the highest value on the chart, used to size the stub when the scale has no maximum.
 */
export function clipToScale(value: number | null, scale: ChartScale, top?: number): ClippedValue {
  if (value === null || !scale.hard) return { drawn: value, fit: 'within' };
  if (scale.max !== undefined && value > scale.max) return { drawn: scale.max, fit: 'above' };
  if (scale.min !== undefined && value < scale.min) {
    const upper = scale.max ?? top ?? scale.min;
    const range = upper > scale.min ? upper - scale.min : 1;
    return { drawn: scale.min + range * BELOW_MIN_STUB, fit: 'below' };
  }
  return { drawn: value, fit: 'within' };
}

/** Text for a hard scale's bounds, e.g. "60 to 150", "from 60", "up to 150"; null for soft scales. */
export function scaleBoundsText(metric: MetricInfo, scale: ChartScale): string | null {
  if (!scale.hard) return null;
  const min = scale.min === undefined ? null : formatTick(metric, scale.min);
  const max = scale.max === undefined ? null : formatTick(metric, scale.max);
  if (min !== null && max !== null) return `${min} to ${max}`;
  if (min !== null) return `from ${min}`;
  if (max !== null) return `up to ${max}`;
  return null;
}
