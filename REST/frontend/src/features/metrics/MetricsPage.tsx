import { ChartPanel } from './components/ChartPanel';
import { SelectionsPanel } from './components/SelectionsPanel';
import { SAY_DO_RATIO } from './metricDefinitions';
import { useMetricsExplorer } from './useMetricsExplorer';

const metric = SAY_DO_RATIO;

export function MetricsPage() {
  const explorer = useMetricsExplorer(metric);

  return (
    <div className="metrics-page">
      <SelectionsPanel
        focusAreas={explorer.focusAreas}
        focusLevels={explorer.focusLevels}
        selectedArea={explorer.selectedArea}
        selectedLevel={explorer.selectedLevel}
        filters={explorer.filters}
        loading={explorer.loading}
        error={explorer.error}
        onSelectArea={explorer.selectArea}
        onSelectLevel={explorer.selectLevel}
        onRemoveFilter={explorer.removeFilter}
        onClearFilters={explorer.clearFilters}
      />
      <ChartPanel
        metric={metric}
        selectedLevel={explorer.selectedLevel}
        rows={explorer.rows}
        loading={explorer.loading}
        error={explorer.chartError}
        onBarClick={explorer.drillDown}
      />
    </div>
  );
}
