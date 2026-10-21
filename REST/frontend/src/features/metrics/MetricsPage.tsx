import { ChartPanel } from './components/ChartPanel';
import { SelectionsPanel } from './components/SelectionsPanel';
import { useMetricList } from './useMetricList';
import { useMetricsExplorer } from './useMetricsExplorer';

/** Metric picker + drill-down explorer. Everything about a metric comes from the API (Metrics table). */
export function MetricsPage() {
  const metricList = useMetricList();
  const explorer = useMetricsExplorer(metricList.selectedMetric);

  return (
    <div className="metrics-page">
      <SelectionsPanel
        metrics={metricList.metrics}
        selectedMetric={metricList.selectedMetric}
        onSelectMetric={metricList.selectMetric}
        focusAreas={explorer.focusAreas}
        focusLevels={explorer.focusLevels}
        selectedArea={explorer.selectedArea}
        selectedLevel={explorer.selectedLevel}
        filters={explorer.filters}
        loading={explorer.loading || metricList.loading}
        error={metricList.error ?? explorer.error}
        onSelectArea={explorer.selectArea}
        onSelectLevel={explorer.selectLevel}
        onRemoveFilter={explorer.removeFilter}
        onClearFilters={explorer.clearFilters}
      />
      <ChartPanel
        metric={metricList.selectedMetric}
        selectedLevel={explorer.selectedLevel}
        rows={explorer.rows}
        loading={explorer.loading || metricList.loading}
        error={explorer.chartError}
        onBarClick={explorer.drillDown}
      />
    </div>
  );
}
