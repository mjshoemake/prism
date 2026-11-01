import type { Dashboard, DashboardChart, DashboardLayout } from '../../api/types';

/** The three layouts, in the order the edit page shows them. */
export const LAYOUTS: readonly { layout: DashboardLayout; label: string; icon: string }[] = [
  { layout: 1, label: '1 chart', icon: 'fa-square' },
  { layout: 2, label: '2 charts', icon: 'fa-grip-lines' },
  { layout: 4, label: '4 charts', icon: 'fa-th-large' },
];

/** Name of a layout for lists: "1 chart", "2 charts (stacked)", "4 charts (quadrants)". */
export function layoutName(layout: DashboardLayout): string {
  switch (layout) {
    case 1:
      return '1 chart';
    case 2:
      return '2 charts (stacked)';
    default:
      return '4 charts (quadrants)';
  }
}

/** Positions shown in a layout: 1 → [1]; 2 → [1, 2] (top, bottom); 4 → [1, 2, 3, 4] (reading order). */
export function visiblePositions(layout: DashboardLayout): number[] {
  return Array.from({ length: layout }, (_, i) => i + 1);
}

const PLACES: Record<DashboardLayout, readonly string[]> = {
  1: [''],
  2: ['top', 'bottom'],
  4: ['top-left', 'top-right', 'bottom-left', 'bottom-right'],
};

/** Name of a chart area on the edit page: "Chart", "Chart 1 — top", "Chart 3 — bottom-left". */
export function areaLabel(layout: DashboardLayout, position: number): string {
  const place = PLACES[layout][position - 1];
  return place ? `Chart ${position} — ${place}` : 'Chart';
}

/** The chart area at a position, if it has a metric. Areas beyond the layout keep theirs (hidden, not dropped). */
export function chartAt(dashboard: Dashboard, position: number): DashboardChart | null {
  return dashboard.charts.find((c) => c.position === position) ?? null;
}
