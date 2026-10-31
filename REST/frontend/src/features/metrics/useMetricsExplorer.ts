import { useCallback, useEffect, useRef, useState } from 'react';
import { ApiError, errorMessage, isAbortError } from '../../api/client';
import { metricsApi } from '../../api/metricsApi';
import type { ChartRow, FocusArea, FocusLevel, MetricInfo } from '../../api/types';
import { useToast } from '../../components/toast/useToast';
import { formatLabel, hasFilter, toRequestFilters, type ChartBar, type Filter } from './filters';

/** One chart's data: its rows, whether they are loading, and an error (e.g. the metric can't use this level). */
export interface ChartState {
  rows: ChartRow[];
  loading: boolean;
  error: string | null;
}

const EMPTY_CHART: ChartState = { rows: [], loading: false, error: null };

/**
 * State and actions for the drill-down metrics explorer, shared by every chart on the page:
 *
 *   focus area ──▶ focus levels ──▶ chart rows per metric (grouped by the selected level, filtered by `filters`)
 *
 * `metrics` has one entry per chart area (null = no metric chosen there); pass a stable array (useMemo), since a new
 * array reloads every chart. The Metrics page passes one metric, a dashboard one per visible chart area. A bar click
 * on any chart adds the filter and drills down, so all charts change together (owner, 2026-10-31).
 * Clicking a bar adds "<level> = <bar>" as a filter (the bar's key is sent, its label shown) and drills down to the level's drill target
 * (`drill_to_focus_level_pk`). A level without one doesn't drill: the click only adds the filter. A focus area can
 * hold several drill paths (e.g. Year → Year & Quarter → … and Quarter → Month → Sprint); the order levels are listed
 * in is for display only.
 * The last level chosen in each focus area is remembered for when the user switches back.
 * Filters are kept when the metric changes, so the same selection can be viewed through another metric.
 * Requests carry only IDs and values; the server builds the SQL.
 */
export function useMetricsExplorer(metrics: readonly (MetricInfo | null)[]) {
  const { showToast } = useToast();

  const [focusAreas, setFocusAreas] = useState<FocusArea[]>([]);
  const [focusLevels, setFocusLevels] = useState<FocusLevel[]>([]);
  const [selectedArea, setSelectedArea] = useState<FocusArea | null>(null);
  const [selectedLevel, setSelectedLevel] = useState<FocusLevel | null>(null);
  const [filters, setFilters] = useState<Filter[]>([]);
  const [charts, setCharts] = useState<ChartState[]>([]);

  const [areasLoading, setAreasLoading] = useState(true);
  const [levelsLoading, setLevelsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  /** focus_area_pk → focus_level_pk of the last level selected in that area. */
  const lastLevelByArea = useRef<Record<number, number>>({});

  // 1. Focus areas: load once, select the first.
  useEffect(() => {
    const controller = new AbortController();
    setAreasLoading(true);
    metricsApi
      .getFocusAreas(controller.signal)
      .then((areas) => {
        setFocusAreas(areas);
        if (areas.length > 0) setSelectedArea(areas[0]);
      })
      .catch((e) => {
        if (!isAbortError(e)) setError(`Failed to fetch focus areas: ${errorMessage(e)}`);
      })
      .finally(() => {
        if (!controller.signal.aborted) setAreasLoading(false);
      });
    return () => controller.abort();
  }, []);

  // 2. Focus levels of the selected area: select the remembered level, else the first.
  useEffect(() => {
    if (!selectedArea) return;
    const controller = new AbortController();
    setLevelsLoading(true);
    metricsApi
      .getFocusLevels(selectedArea.focus_area_pk, controller.signal)
      .then((levels) => {
        setFocusLevels(levels);
        if (levels.length > 0) {
          const remembered = lastLevelByArea.current[selectedArea.focus_area_pk];
          setSelectedLevel(levels.find((l) => l.focus_level_pk === remembered) ?? levels[0]);
        }
      })
      .catch((e) => {
        if (!isAbortError(e)) setError(`Failed to fetch focus levels: ${errorMessage(e)}`);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLevelsLoading(false);
      });
    return () => controller.abort();
  }, [selectedArea]);

  // 3. Chart rows, one request per chart: reload whenever the metrics, the level or the filters change.
  useEffect(() => {
    if (!selectedLevel) return;
    const controller = new AbortController();
    setCharts(metrics.map((m) => ({ rows: [], loading: m !== null, error: null })));
    metrics.forEach((metric, index) => {
      if (!metric) return;
      metricsApi
        .getChartData(
          {
            metric_pk: metric.metric_pk,
            focus_level_pk: selectedLevel.focus_level_pk,
            filters: toRequestFilters(filters),
          },
          controller.signal,
        )
        .then((rows) => setCharts((prev) => replaceAt(prev, index, { rows, loading: false, error: null })))
        .catch((e) => {
          if (isAbortError(e)) return;
          setCharts((prev) =>
            replaceAt(prev, index, { rows: [], loading: false, error: `Failed to fetch chart data: ${serverMessage(e)}` }),
          );
        });
    });
    return () => controller.abort();
  }, [selectedLevel, filters, metrics]);

  const selectArea = useCallback((area: FocusArea) => {
    setSelectedArea(area);
    setSelectedLevel(null);
    setCharts((prev) => prev.map(() => EMPTY_CHART));
  }, []);

  const selectLevel = useCallback(
    (level: FocusLevel) => {
      setSelectedLevel(level);
      rememberLevel(lastLevelByArea.current, selectedArea, level);
    },
    [selectedArea],
  );

  /** Bar click: filter on the clicked value, then move to the level's drill target, if it has one. */
  const drillDown = useCallback(
    (bar: ChartBar) => {
      if (!selectedLevel) return;

      if (hasFilter(filters, selectedLevel.focus_level_pk, bar.key)) {
        showToast('Filter already exists', 'warning');
      } else {
        setFilters((prev) => [
          ...prev,
          { focus_level_pk: selectedLevel.focus_level_pk, name: selectedLevel.name, value: bar.key, label: bar.label },
        ]);
        showToast(`Added filter: ${selectedLevel.name} = ${formatLabel(bar.label)}`, 'success');
      }

      const nextLevel = nextDrillLevel(focusLevels, selectedLevel);
      if (nextLevel) {
        setSelectedLevel(nextLevel);
        rememberLevel(lastLevelByArea.current, selectedArea, nextLevel);
        showToast(`Drilled down to: ${nextLevel.name}`, 'info');
      } else {
        showToast(`${selectedLevel.name} doesn't drill down further`, 'info');
      }
    },
    [filters, focusLevels, selectedArea, selectedLevel, showToast],
  );

  const removeFilter = useCallback(
    (index: number) => {
      const removed = filters[index];
      if (!removed) return;
      setFilters((prev) => prev.filter((_, i) => i !== index));
      showToast(`Removed filter: ${removed.name} = ${formatLabel(removed.label)}`, 'info');
    },
    [filters, showToast],
  );

  const clearFilters = useCallback(() => {
    setFilters([]);
    showToast('Cleared all filters', 'info');
  }, [showToast]);

  return {
    focusAreas,
    focusLevels,
    selectedArea,
    selectedLevel,
    filters,
    /** One entry per metric passed in, in the same order. */
    charts: metrics.map((_, i) => charts[i] ?? EMPTY_CHART),
    loading: areasLoading || levelsLoading || charts.some((c) => c.loading),
    error,
    selectArea,
    selectLevel,
    drillDown,
    removeFilter,
    clearFilters,
  };
}

/** The level a bar click on `level` drills down to: its drill target, if that level is loaded; else none. */
export function nextDrillLevel(levels: readonly FocusLevel[], level: FocusLevel): FocusLevel | undefined {
  if (level.drill_to_focus_level_pk == null) return undefined;
  return levels.find((l) => l.focus_level_pk === level.drill_to_focus_level_pk);
}

function replaceAt(charts: readonly ChartState[], index: number, chart: ChartState): ChartState[] {
  const next = [...charts];
  next[index] = chart;
  return next;
}

/** The server's {success:false, message} text for a 400, else the general error text. */
function serverMessage(error: unknown): string {
  if (error instanceof ApiError) {
    const body = error.json<{ message?: string }>();
    if (body?.message) return body.message;
  }
  return errorMessage(error);
}

function rememberLevel(memory: Record<number, number>, area: FocusArea | null, level: FocusLevel): void {
  if (area) {
    memory[area.focus_area_pk] = level.focus_level_pk;
  }
}
