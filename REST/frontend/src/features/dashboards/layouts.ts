import type { Dashboard, DashboardChart, DashboardLayout } from '../../api/types';

/** The three layouts, in the order the switch shows them. */
export const LAYOUTS: readonly { layout: DashboardLayout; label: string; icon: string }[] = [
  { layout: 1, label: '1 chart', icon: 'fa-square' },
  { layout: 2, label: '2 charts', icon: 'fa-grip-lines' },
  { layout: 4, label: '4 charts', icon: 'fa-th-large' },
];

/** Positions shown in a layout: 1 → [1]; 2 → [1, 2] (top, bottom); 4 → [1, 2, 3, 4] (reading order). */
export function visiblePositions(layout: DashboardLayout): number[] {
  return Array.from({ length: layout }, (_, i) => i + 1);
}

/** The metric chosen for a chart area, if any. Areas beyond the layout keep theirs (hidden, not dropped). */
export function metricAt(dashboard: Dashboard, position: number): number | null {
  return dashboard.charts.find((c) => c.position === position)?.metric_pk ?? null;
}

/** The dashboard's charts with `position` set to `metricPk` (or cleared when null), in position order. */
export function withMetric(charts: readonly DashboardChart[], position: number, metricPk: number | null): DashboardChart[] {
  const others = charts.filter((c) => c.position !== position);
  const next = metricPk === null ? others : [...others, { position, metric_pk: metricPk }];
  return next.sort((a, b) => a.position - b.position);
}
