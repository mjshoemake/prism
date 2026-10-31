import { useMemo } from 'react';
import { useAuth } from '../../auth/useAuth';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { useMetricList } from '../metrics/useMetricList';
import { useMetricsExplorer } from '../metrics/useMetricsExplorer';
import { DashboardChartCard } from './components/DashboardChartCard';
import { DashboardSidebar } from './components/DashboardSidebar';
import { metricAt, visiblePositions } from './layouts';
import { useDashboards } from './useDashboards';

/**
 * A saved dashboard of 1, 2 or 4 charts (SQL/020). Each chart area has its own metric; the focus area, level and
 * filters are shared, so a bar click on any chart filters and drills down every chart (owner, 2026-10-31).
 */
export function DashboardsPage() {
  const { userId } = useAuth();
  const metricList = useMetricList();
  const board = useDashboards(userId, metricList.metrics[0]?.metric_pk ?? null);
  const selected = board.selected;

  const positions = useMemo(() => (selected ? visiblePositions(selected.layout) : []), [selected]);
  // One entry per visible chart area, stable while the dashboard and metric list don't change.
  const chartMetrics = useMemo(
    () =>
      positions.map((p) => {
        const pk = selected ? metricAt(selected, p) : null;
        return metricList.metrics.find((m) => m.metric_pk === pk) ?? null;
      }),
    [positions, selected, metricList.metrics],
  );
  const explorer = useMetricsExplorer(chartMetrics);

  const renderCharts = () => {
    if (board.loading || metricList.loading) return <LoadingSpinner caption="Loading dashboards..." />;
    if (!selected) {
      return (
        <div className="text-center py-5">
          <i className="fas fa-th-large fa-3x text-muted mb-3" />
          <p className="text-muted">No dashboards yet. Create one in the panel on the left.</p>
        </div>
      );
    }
    return (
      <div className={`dashboard-grid dashboard-grid--${selected.layout}`}>
        {positions.map((position, i) => (
          <DashboardChartCard
            key={position}
            position={position}
            metrics={metricList.metrics}
            metric={chartMetrics[i]}
            selectedLevel={explorer.selectedLevel}
            chart={explorer.charts[i]}
            onSelectMetric={(m) => board.setMetric(position, m.metric_pk)}
            onBarClick={explorer.drillDown}
          />
        ))}
      </div>
    );
  };

  return (
    <div className="metrics-page">
      <DashboardSidebar
        dashboards={board.dashboards}
        selected={selected}
        onSelect={board.select}
        onCreate={board.create}
        onRename={board.rename}
        onDelete={() => void board.remove()}
        onSetLayout={board.setLayout}
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
      <section className="metrics-content">{renderCharts()}</section>
    </div>
  );
}
