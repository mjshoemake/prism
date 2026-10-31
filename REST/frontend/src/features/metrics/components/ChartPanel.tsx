import type { ChartRow, FocusLevel, MetricInfo } from '../../../api/types';
import type { ChartBar } from '../filters';
import { ErrorAlert } from '../../../components/ErrorAlert';
import { LoadingSpinner } from '../../../components/LoadingSpinner';
import { MetricBarChart } from './MetricBarChart';

interface ChartPanelProps {
  metric: MetricInfo | null;
  selectedLevel: FocusLevel | null;
  rows: ChartRow[];
  loading: boolean;
  error: string | null;
  onBarClick: (bar: ChartBar) => void;
}

export function ChartPanel({ metric, selectedLevel, rows, loading, error, onBarClick }: ChartPanelProps) {
  const renderBody = () => {
    if (error) return <ErrorAlert message={error} />;
    if (loading) return <LoadingSpinner caption="Loading chart data..." />;
    if (rows.length === 0 || !selectedLevel || !metric) {
      return (
        <div className="text-center py-5">
          <i className="fas fa-chart-bar fa-3x text-muted mb-3" />
          <p className="text-muted">No data available for the selected criteria.</p>
        </div>
      );
    }
    return (
      <div className="h-100">
        <MetricBarChart
          rows={rows}
          levelName={selectedLevel.name}
          metric={metric}
          onBarClick={onBarClick}
        />
      </div>
    );
  };

  return (
    <section className="metrics-content">
      <div className="card shadow-purple h-100">
        <div className="card-header">
          <h5 className="mb-0">
            <i className="fas fa-chart-bar me-2" />
            {metric?.title ?? 'Metric'} by {selectedLevel?.name ?? 'Focus Level'}
          </h5>
        </div>
        <div className="card-body chart-card-body">{renderBody()}</div>
      </div>
    </section>
  );
}
