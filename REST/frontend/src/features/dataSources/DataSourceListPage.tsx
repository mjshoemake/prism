import { useState } from 'react';
import { Link, useNavigate } from 'react-router';
import { serverMessage } from '../../api/client';
import { dataSourcesApi } from '../../api/dataSourcesApi';
import type { ConnectionTestResult, DataSource } from '../../api/types';
import { useAuth } from '../../auth/useAuth';
import { ErrorAlert } from '../../components/ErrorAlert';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { Modal } from '../../components/Modal';
import { useToast } from '../../components/toast/useToast';
import { ConnectionTestPanel } from './components/ConnectionTestPanel';
import { connectionSummary } from './labels';
import { useDataSources } from './useDataSources';

/**
 * Metrics → Data Sources: the enterprise's data sources (where each STAR schema lives and how Prism connects), with how
 * many facts, dimensions and metrics belong to each; New / Test / Edit / Delete. A data source is the parent of its
 * facts and dimensions, so it can only be deleted once those are gone.
 */
export function DataSourceListPage() {
  const { enterprisePk } = useAuth();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const board = useDataSources(enterprisePk);
  const [deleting, setDeleting] = useState<DataSource | null>(null);
  const [deleteError, setDeleteError] = useState<string | null>(null);
  const [testing, setTesting] = useState<{ source: DataSource; result: ConnectionTestResult | null } | null>(null);
  const [busy, setBusy] = useState(false);

  if (enterprisePk === null) {
    return (
      <div className="container py-3">
        <ErrorAlert message="Your session has no enterprise. Sign out and sign in again." />
      </div>
    );
  }

  const confirmDelete = async () => {
    if (!deleting) return;
    setBusy(true);
    try {
      await board.remove(deleting.data_source_pk);
      showToast(`Deleted data source: ${deleting.name}`, 'info');
      setDeleting(null);
    } catch (e) {
      setDeleteError(serverMessage(e));
    } finally {
      setBusy(false);
    }
  };

  const test = async (source: DataSource) => {
    setTesting({ source, result: null });
    try {
      const result = await dataSourcesApi.test(enterprisePk, source.data_source_pk);
      setTesting({ source, result });
    } catch (e) {
      setTesting({ source, result: { success: false, message: serverMessage(e), readable: [], tables: [] } });
    }
  };

  const renderBody = () => {
    if (board.loading && board.dataSources.length === 0) return <LoadingSpinner />;
    if (board.error) return <ErrorAlert message={board.error} className="m-3" />;
    if (board.dataSources.length === 0) {
      return <p className="text-muted text-center py-4 mb-0">No data sources yet. Use "New data source" to add one.</p>;
    }
    return (
      <div className="table-responsive">
        <table className="table table-dark table-striped table-hover align-middle mb-0 list-table data-sources-table">
          <thead>
            <tr>
              <th scope="col">Name</th>
              <th scope="col">Connection</th>
              <th scope="col" className="text-end">Facts</th>
              <th scope="col" className="text-end">Dimensions</th>
              <th scope="col" className="text-end">Metrics</th>
              <th scope="col" className="text-end">
                <span className="visually-hidden">Actions</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {board.dataSources.map((d) => (
              <tr key={d.data_source_pk}>
                <td>
                  {d.name}
                  {d.description && <div className="small text-muted">{d.description}</div>}
                </td>
                <td>{connectionSummary(d)}</td>
                <td className="text-end">{d.fact_count}</td>
                <td className="text-end">{d.dimension_count}</td>
                <td className="text-end">{d.metric_count}</td>
                <td className="text-end text-nowrap">
                  <button
                    type="button"
                    className="btn btn-sm btn-outline-secondary me-1"
                    aria-label={`Test connection to ${d.name}`}
                    title="Test connection"
                    onClick={() => void test(d)}
                  >
                    <i className="fas fa-plug" aria-hidden="true" />
                  </button>
                  <Link
                    className="btn btn-sm btn-outline-secondary me-1"
                    to={`/data-sources/${d.data_source_pk}/edit`}
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
                    onClick={() => {
                      setDeleteError(null);
                      setDeleting(d);
                    }}
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
            Data Sources <span className="badge text-bg-secondary align-middle">{board.dataSources.length}</span>
          </h5>
          <button type="button" className="btn btn-primary btn-sm" onClick={() => navigate('/data-sources/new')}>
            <i className="fas fa-plus me-1" aria-hidden="true" />
            New data source
          </button>
        </div>
        <div className="card-body p-0">{renderBody()}</div>
      </div>

      {testing && (
        <Modal
          title={`Test connection: ${testing.source.name}`}
          onClose={() => setTesting(null)}
          footer={
            <button type="button" className="btn btn-primary" onClick={() => setTesting(null)}>
              Close
            </button>
          }
        >
          {testing.result ? <ConnectionTestPanel result={testing.result} /> : <LoadingSpinner caption="Testing..." />}
        </Modal>
      )}

      {deleting && (
        <Modal
          title="Delete data source"
          onClose={() => setDeleting(null)}
          footer={
            <>
              <button type="button" className="btn btn-outline-secondary" onClick={() => setDeleting(null)} disabled={busy}>
                {deleteError ? 'Close' : 'Keep it'}
              </button>
              {!deleteError && (
                <button type="button" className="btn btn-primary" onClick={() => void confirmDelete()} disabled={busy}>
                  Delete
                </button>
              )}
            </>
          }
        >
          {deleteError ? (
            <ErrorAlert message={deleteError} className="mb-0" />
          ) : (
            <p className="mb-0">
              Delete <strong>{deleting.name}</strong>? Only Prism's settings are removed; the database itself is not
              touched.
            </p>
          )}
        </Modal>
      )}
    </div>
  );
}
