import type { MetricInfo } from '../../api/types';

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

export interface AxisBounds {
  min?: number;
  suggestedMin?: number;
  suggestedMax?: number;
}

/**
 * Y-axis bounds for the metric's scale mode:
 * - zero_to_100: 0–100, extended to a round number above the highest value when a value exceeds 100;
 * - fit: from 0 to a round number above the highest value;
 * - fixed: the metric's minimum/maximum as suggestions only. Values outside them still widen the axis, because
 *   bars beyond a hard maximum must be marked (barber pole) rather than cut off, and that isn't built yet.
 */
export function axisBounds(metric: MetricInfo): AxisBounds {
  switch (metric.scale_mode) {
    case 'zero_to_100':
      return { min: 0, suggestedMax: 100 };
    case 'fixed':
      return {
        suggestedMin: metric.scale_min ?? undefined,
        suggestedMax: metric.scale_max ?? undefined,
      };
    default:
      return {};
  }
}
