import type { DashboardChart, FocusLevel, MetricInfo } from '../../../api/types';
import { ErrorAlert } from '../../../components/ErrorAlert';
import { LoadingSpinner } from '../../../components/LoadingSpinner';
import { MetricBarChart } from '../../metrics/components/MetricBarChart';
import type { ChartBar } from '../../metrics/filters';
import { effectiveScale, scaleBoundsText } from '../../metrics/metricFormat';
import type { ChartState } from '../../metrics/useMetricsExplorer';

interface DashboardChartCardProps {
  position: number;
  /** The metric chosen for this area; null = none (choose one on the dashboard's Edit page). */
  metric: MetricInfo | null;
  /** The saved chart area (its scale); null when the area has no metric. */
  area: DashboardChart | null;
  selectedLevel: FocusLevel | null;
  chart: ChartState;
  onBarClick: (bar: ChartBar) => void;
}

/**
 * One chart area of a dashboard on the Analysis page: a header naming what it shows ("Say/Do Ratio by Sprint"), and
 * the chart grouped by the dashboard's shared level. The metric and scale are changed on the Edit page only.
 */
export function DashboardChartCard({ position, metric, area, selectedLevel, chart, onBarClick }: DashboardChartCardProps) {
  const scale = metric ? effectiveScale(metric, area) : null;
  const bounds = metric && scale ? scaleBoundsText(metric, scale) : null;

  const renderBody = () => {
    if (!metric || !scale) {
      return <p className="text-muted text-center py-4">No metric chosen. Use Edit to choose one.</p>;
    }
    if (chart.error) return <ErrorAlert message={chart.error} />;
    if (chart.loading) return <LoadingSpinner caption="Loading chart data..." />;
    if (chart.rows.length === 0 || !selectedLevel) {
      return <p className="text-muted text-center py-4">No data available for the selected criteria.</p>;
    }
    return (
      <MetricBarChart rows={chart.rows} levelName={selectedLevel.name} metric={metric} scale={scale} onBarClick={onBarClick} />
    );
  };

  return (
    <section className="card shadow-purple page-card dashboard-chart" data-position={position} aria-label={chartTitle(metric, selectedLevel)}>
      <div className="card-header d-flex align-items-center gap-2">
        <h2 className="dashboard-chart__title mb-0">{chartTitle(metric, selectedLevel)}</h2>
        {bounds && <span className="badge text-bg-secondary ms-auto dashboard-chart__scale">Axis {bounds}</span>}
      </div>
      <div className="card-body dashboard-chart__body">{renderBody()}</div>
    </section>
  );
}

/** "Say/Do Ratio by Sprint", or "Say/Do Ratio" before a level is loaded, or "No metric". */
function chartTitle(metric: MetricInfo | null, level: FocusLevel | null): string {
  if (!metric) return 'No metric';
  return level ? `${metric.title} by ${level.name}` : metric.title;
}
