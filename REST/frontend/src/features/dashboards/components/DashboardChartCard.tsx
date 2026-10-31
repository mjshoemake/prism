import type { FocusLevel, MetricInfo } from '../../../api/types';
import { DropdownSelect } from '../../../components/DropdownSelect';
import { ErrorAlert } from '../../../components/ErrorAlert';
import { LoadingSpinner } from '../../../components/LoadingSpinner';
import { MetricBarChart } from '../../metrics/components/MetricBarChart';
import type { ChartBar } from '../../metrics/filters';
import type { ChartState } from '../../metrics/useMetricsExplorer';

interface DashboardChartCardProps {
  position: number;
  metrics: MetricInfo[];
  /** The metric chosen for this area; null = none yet. */
  metric: MetricInfo | null;
  selectedLevel: FocusLevel | null;
  chart: ChartState;
  onSelectMetric: (metric: MetricInfo) => void;
  onBarClick: (bar: ChartBar) => void;
}

/** One chart area of a dashboard: its own metric picker, and the chart grouped by the dashboard's shared level. */
export function DashboardChartCard({
  position,
  metrics,
  metric,
  selectedLevel,
  chart,
  onSelectMetric,
  onBarClick,
}: DashboardChartCardProps) {
  const renderBody = () => {
    if (!metric) return <p className="text-muted text-center py-4">Choose a metric for this chart.</p>;
    if (chart.error) return <ErrorAlert message={chart.error} />;
    if (chart.loading) return <LoadingSpinner caption="Loading chart data..." />;
    if (chart.rows.length === 0 || !selectedLevel) {
      return <p className="text-muted text-center py-4">No data available for the selected criteria.</p>;
    }
    return <MetricBarChart rows={chart.rows} levelName={selectedLevel.name} metric={metric} onBarClick={onBarClick} />;
  };

  return (
    <div className="card shadow-purple dashboard-chart" data-position={position}>
      <div className="card-header d-flex align-items-center gap-2">
        <div className="dashboard-chart__picker">
          <DropdownSelect
            id={`dashboardChart${position}Metric`}
            ariaLabel={`Chart ${position} metric`}
            placeholder="Select Metric"
            items={metrics}
            selected={metric}
            getKey={(m) => m.metric_pk}
            getLabel={(m) => m.title}
            onSelect={onSelectMetric}
            disabled={metrics.length === 0}
          />
        </div>
        {metric && selectedLevel && (
          <span className="text-muted small ms-auto">
            {metric.title} by {selectedLevel.name}
          </span>
        )}
      </div>
      <div className="card-body dashboard-chart__body">{renderBody()}</div>
    </div>
  );
}
