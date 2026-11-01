import { useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router';
import type { Dashboard } from '../../api/types';
import { useAuth } from '../../auth/useAuth';
import { ErrorAlert } from '../../components/ErrorAlert';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { DashboardChartCard } from '../dashboards/components/DashboardChartCard';
import { chartAt, visiblePositions } from '../dashboards/layouts';
import { isConflict, serverMessage, useDashboards } from '../dashboards/useDashboards';
import { useMetricList } from '../metrics/useMetricList';
import { useMetricsExplorer } from '../metrics/useMetricsExplorer';
import { AnalysisSidebar } from './components/AnalysisSidebar';
import { loadLastDashboard, saveLastDashboard } from './lastDashboard';

/** Name of the dashboard created for a user who has none, so the page always shows a chart. */
export const FIRST_DASHBOARD_NAME = 'My dashboard';

/**
 * Analysis: one of the user's saved dashboards (1, 2 or 4 charts). Each chart area shows its saved metric with its
 * saved y-axis scale; the focus area, level and filters are shared, so a bar click on any chart filters and drills
 * down every chart (owner, 2026-10-31). Metrics and scales are changed on the dashboard's Edit page, never here.
 * The open dashboard is in the URL (/#/analysis?dashboard=5) and remembered for the next visit.
 */
export function AnalysisPage() {
  const { userId } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const metricList = useMetricList();
  const board = useDashboards(userId);
  const [createError, setCreateError] = useState<string | null>(null);
  const creating = useRef(false);

  const requestedPk = Number(searchParams.get('dashboard')) || null;
  const selected = pickDashboard(board.dashboards, requestedPk, userId ? loadLastDashboard(userId) : null);

  // Keep the URL and the remembered dashboard in step with what is shown.
  useEffect(() => {
    if (!selected || !userId) return;
    saveLastDashboard(userId, selected.dashboard_pk);
    if (requestedPk !== selected.dashboard_pk) {
      setSearchParams({ dashboard: String(selected.dashboard_pk) }, { replace: true });
    }
  }, [requestedPk, selected, setSearchParams, userId]);

  // A user without dashboards gets one: layout 1 with the first metric, so a chart always shows.
  const firstMetric = metricList.metrics[0] ?? null;
  const needsFirstDashboard =
    !board.loading && !board.error && board.dashboards.length === 0 && !metricList.loading && firstMetric !== null;
  useEffect(() => {
    if (!needsFirstDashboard || !firstMetric || creating.current || createError) return;
    creating.current = true; // StrictMode runs effects twice; create only once.
    board
      .create({
        name: FIRST_DASHBOARD_NAME,
        layout: 1,
        charts: [{ position: 1, metric_pk: firstMetric.metric_pk, y_axis_mode: 'metric', y_axis_min: null, y_axis_max: null }],
      })
      .then((created) => setSearchParams({ dashboard: String(created.dashboard_pk) }, { replace: true }))
      .catch((e) => {
        // Another tab created it first: show that one.
        if (isConflict(e)) board.reload();
        else setCreateError(`Couldn't create your first dashboard: ${serverMessage(e)}`);
      })
      .finally(() => {
        creating.current = false;
      });
  }, [board, createError, firstMetric, needsFirstDashboard, setSearchParams]);

  const positions = useMemo(() => (selected ? visiblePositions(selected.layout) : []), [selected]);
  const areas = useMemo(() => positions.map((p) => (selected ? chartAt(selected, p) : null)), [positions, selected]);
  // One entry per visible chart area, stable while the dashboard and metric list don't change.
  const chartMetrics = useMemo(
    () => areas.map((area) => metricList.metrics.find((m) => m.metric_pk === area?.metric_pk) ?? null),
    [areas, metricList.metrics],
  );
  const explorer = useMetricsExplorer(chartMetrics);

  const select = (dashboard: Dashboard) => setSearchParams({ dashboard: String(dashboard.dashboard_pk) });
  const edit = () => {
    if (selected) navigate(`/dashboards/${selected.dashboard_pk}/edit?from=analysis`);
  };

  const renderCharts = () => {
    if (createError) return <ErrorAlert message={createError} />;
    if (board.loading || metricList.loading || needsFirstDashboard) return <LoadingSpinner caption="Loading dashboard..." />;
    if (!selected) {
      return (
        <p className="text-muted text-center py-5">
          {metricList.metrics.length === 0 ? 'No metrics are set up yet.' : 'No dashboard to show.'}
        </p>
      );
    }
    return (
      <div className={`dashboard-grid dashboard-grid--${selected.layout}`}>
        {positions.map((position, i) => (
          <DashboardChartCard
            key={position}
            position={position}
            metric={chartMetrics[i]}
            area={areas[i]}
            selectedLevel={explorer.selectedLevel}
            chart={explorer.charts[i]}
            onBarClick={explorer.drillDown}
          />
        ))}
      </div>
    );
  };

  return (
    <div className="metrics-page">
      <AnalysisSidebar
        dashboards={board.dashboards}
        selected={selected}
        onSelect={select}
        onEdit={edit}
        focusAreas={explorer.focusAreas}
        focusLevels={explorer.focusLevels}
        selectedArea={explorer.selectedArea}
        selectedLevel={explorer.selectedLevel}
        filters={explorer.filters}
        loading={explorer.loading || board.loading}
        error={board.error ?? metricList.error ?? explorer.error}
        onSelectArea={explorer.selectArea}
        onSelectLevel={explorer.selectLevel}
        onRemoveFilter={explorer.removeFilter}
        onClearFilters={explorer.clearFilters}
      />
      <section className="metrics-content" aria-label="Charts">
        {renderCharts()}
      </section>
    </div>
  );
}

/** The dashboard asked for in the URL, else the one last opened, else the first (by name); null if there are none. */
export function pickDashboard(dashboards: readonly Dashboard[], requested: number | null, last: number | null): Dashboard | null {
  return (
    dashboards.find((d) => d.dashboard_pk === requested) ??
    dashboards.find((d) => d.dashboard_pk === last) ??
    dashboards[0] ??
    null
  );
}
