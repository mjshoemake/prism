import { useCallback, useEffect, useRef, useState } from 'react';
import { errorMessage, isAbortError } from '../../api/client';
import { metricsApi } from '../../api/metricsApi';
import type { ChartRow, FocusArea, FocusLevel, MetricInfo } from '../../api/types';
import { useToast } from '../../components/toast/useToast';
import { formatLabel, hasFilter, toRequestFilters, type Filter } from './filters';

/**
 * State and actions for the drill-down metrics explorer:
 *
 *   focus area ──▶ focus levels ──▶ chart rows (grouped by the selected level, filtered by `filters`)
 *
 * Clicking a bar adds "<level> = <bar label>" as a filter and drills down to the next focus level.
 * The last level chosen in each focus area is remembered for when the user switches back.
 * Filters are kept when the metric changes, so the same selection can be viewed through another metric.
 * Requests carry only IDs and values; the server builds the SQL.
 */
export function useMetricsExplorer(metric: MetricInfo | null) {
  const { showToast } = useToast();

  const [focusAreas, setFocusAreas] = useState<FocusArea[]>([]);
  const [focusLevels, setFocusLevels] = useState<FocusLevel[]>([]);
  const [selectedArea, setSelectedArea] = useState<FocusArea | null>(null);
  const [selectedLevel, setSelectedLevel] = useState<FocusLevel | null>(null);
  const [filters, setFilters] = useState<Filter[]>([]);
  const [rows, setRows] = useState<ChartRow[]>([]);

  const [areasLoading, setAreasLoading] = useState(true);
  const [levelsLoading, setLevelsLoading] = useState(false);
  const [rowsLoading, setRowsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [chartError, setChartError] = useState<string | null>(null);

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

  // 3. Chart rows: reload whenever the metric, the level or the filters change.
  useEffect(() => {
    if (!selectedLevel || !metric) return;
    const controller = new AbortController();
    setRowsLoading(true);
    setChartError(null);
    metricsApi
      .getChartData(
        {
          metric_pk: metric.metric_pk,
          focus_level_pk: selectedLevel.focus_level_pk,
          filters: toRequestFilters(filters),
        },
        controller.signal,
      )
      .then(setRows)
      .catch((e) => {
        if (isAbortError(e)) return;
        setChartError(`Failed to fetch chart data: ${errorMessage(e)}`);
        setRows([]);
      })
      .finally(() => {
        if (!controller.signal.aborted) setRowsLoading(false);
      });
    return () => controller.abort();
  }, [selectedLevel, filters, metric]);

  const selectArea = useCallback((area: FocusArea) => {
    setSelectedArea(area);
    setSelectedLevel(null);
    setRows([]);
  }, []);

  const selectLevel = useCallback(
    (level: FocusLevel) => {
      setSelectedLevel(level);
      rememberLevel(lastLevelByArea.current, selectedArea, level);
    },
    [selectedArea],
  );

  /** Bar click: filter on the clicked value, then move to the next (deeper) focus level. */
  const drillDown = useCallback(
    (label: string | null) => {
      if (!selectedLevel) return;

      if (hasFilter(filters, selectedLevel.focus_level_pk, label)) {
        showToast('Filter already exists', 'warning');
      } else {
        setFilters((prev) => [...prev, { focus_level_pk: selectedLevel.focus_level_pk, name: selectedLevel.name, value: label }]);
        showToast(`Added filter: ${selectedLevel.name} = ${formatLabel(label)}`, 'success');
      }

      const index = focusLevels.findIndex((l) => l.focus_level_pk === selectedLevel.focus_level_pk);
      const nextLevel = index >= 0 ? focusLevels[index + 1] : undefined;
      if (nextLevel) {
        setSelectedLevel(nextLevel);
        rememberLevel(lastLevelByArea.current, selectedArea, nextLevel);
        showToast(`Drilled down to: ${nextLevel.name}`, 'info');
      } else {
        showToast('No deeper level available', 'warning');
      }
    },
    [filters, focusLevels, selectedArea, selectedLevel, showToast],
  );

  const removeFilter = useCallback(
    (index: number) => {
      const removed = filters[index];
      if (!removed) return;
      setFilters((prev) => prev.filter((_, i) => i !== index));
      showToast(`Removed filter: ${removed.name} = ${formatLabel(removed.value)}`, 'info');
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
    rows,
    loading: areasLoading || levelsLoading || rowsLoading,
    error,
    chartError,
    selectArea,
    selectLevel,
    drillDown,
    removeFilter,
    clearFilters,
  };
}

function rememberLevel(memory: Record<number, number>, area: FocusArea | null, level: FocusLevel): void {
  if (area) {
    memory[area.focus_area_pk] = level.focus_level_pk;
  }
}
