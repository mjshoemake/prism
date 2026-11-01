import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import type { Dashboard, MetricInfo } from '../../api/types';
import { useAuth } from '../../auth/useAuth';
import { ErrorAlert } from '../../components/ErrorAlert';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { Modal } from '../../components/Modal';
import { useToast } from '../../components/toast/useToast';
import { useMetricList } from '../metrics/useMetricList';
import { chartAt, layoutName, visiblePositions } from './layouts';
import { serverMessage, useDashboards } from './useDashboards';

/**
 * Dashboards (configuration): the signed-in user's dashboards with their layout and metrics, and New / Edit / Delete.
 * Dashboards are viewed on the Analysis page; their metrics and scales are set on the Edit page.
 */
export function DashboardListPage() {
  const { userId } = useAuth();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const board = useDashboards(userId);
  const metricList = useMetricList();
  const [deleting, setDeleting] = useState<Dashboard | null>(null);
  const [busy, setBusy] = useState(false);

  const confirmDelete = async () => {
    if (!deleting) return;
    setBusy(true);
    try {
      await board.remove(deleting.dashboard_pk);
      showToast(`Deleted dashboard: ${deleting.name}`, 'info');
      setDeleting(null);
    } catch (e) {
      showToast(`Not deleted: ${serverMessage(e)}`, 'warning');
    } finally {
      setBusy(false);
    }
  };

  const renderBody = () => {
    if (board.loading && board.dashboards.length === 0) return <LoadingSpinner />;
    if (board.error) return <ErrorAlert message={board.error} className="m-3" />;
    if (board.dashboards.length === 0) {
      return <p className="text-muted text-center py-4 mb-0">No dashboards yet. Use "New dashboard" to create one.</p>;
    }
    return (
      <div className="table-responsive">
        <table className="table table-dark table-striped table-hover align-middle mb-0 list-table dashboards-table">
          <thead>
            <tr>
              <th scope="col">Name</th>
              <th scope="col">Layout</th>
              <th scope="col">Metrics</th>
              <th scope="col" className="text-end">
                <span className="visually-hidden">Actions</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {board.dashboards.map((d) => (
              <tr key={d.dashboard_pk}>
                <td>{d.name}</td>
                <td className="text-nowrap">{layoutName(d.layout)}</td>
                <td>{metricTitles(d, metricList.metrics)}</td>
                <td className="text-end text-nowrap">
                  <Link
                    className="btn btn-sm btn-outline-secondary me-1"
                    to={`/analysis?dashboard=${d.dashboard_pk}`}
                    aria-label={`Open ${d.name} in Analysis`}
                    title="Open in Analysis"
                  >
                    <i className="fas fa-chart-bar" aria-hidden="true" />
                  </Link>
                  <Link
                    className="btn btn-sm btn-outline-secondary me-1"
                    to={`/dashboards/${d.dashboard_pk}/edit`}
                    aria-label={`Edit ${d.name}`}
                    title="Edit"
                  >
                    <i className="fas fa-pen" aria-hidden="true" />
                  </Link>
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-secondary"
                    aria-label={`Delete ${d.name}`}
                    title="Delete"
                    onClick={() => setDeleting(d)}
                  >
                    <i className="fas fa-trash" aria-hidden="true" />
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  };

  return (
    <div className="container py-3 w-100">
      <div className="card shadow-purple page-card">
        <div className="card-header d-flex flex-wrap gap-2 align-items-center">
          <h5 className="mb-0 me-auto">
            Dashboards <span className="badge text-bg-secondary align-middle">{board.dashboards.length}</span>
          </h5>
          <button type="button" className="btn btn-primary btn-sm" onClick={() => navigate('/dashboards/new')}>
            <i className="fas fa-plus me-1" aria-hidden="true" />
            New dashboard
          </button>
        </div>
        <div className="card-body p-0">{renderBody()}</div>
      </div>

      {deleting && (
        <Modal
          title="Delete dashboard"
          onClose={() => setDeleting(null)}
          footer={
            <>
              <button type="button" className="btn btn-outline-secondary" onClick={() => setDeleting(null)} disabled={busy}>
                Keep it
              </button>
              <button type="button" className="btn btn-primary" onClick={() => void confirmDelete()} disabled={busy}>
                Delete
              </button>
            </>
          }
        >
          <p className="mb-0">
            Delete <strong>{deleting.name}</strong> and its charts? This can't be undone.
          </p>
        </Modal>
      )}
    </div>
  );
}

/** The titles of the metrics in a dashboard's visible chart areas, in reading order; "—" when none. */
function metricTitles(dashboard: Dashboard, metrics: readonly MetricInfo[]): string {
  const titles = visiblePositions(dashboard.layout)
    .map((p) => chartAt(dashboard, p))
    .map((area) => (area ? (metrics.find((m) => m.metric_pk === area.metric_pk)?.title ?? `Metric ${area.metric_pk}`) : null))
    .filter((t): t is string => t !== null);
  return titles.length > 0 ? titles.join(', ') : '—';
}
