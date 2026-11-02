import type { ConnectionTestResult } from '../../../api/types';

interface ConnectionTestPanelProps {
  result: ConnectionTestResult;
}

/**
 * The outcome of Test connection: a summary line, what the connection's login can read (tables per schema), then one
 * line per registered fact / dimension table.
 */
export function ConnectionTestPanel({ result }: ConnectionTestPanelProps) {
  return (
    <div className={`alert ${result.success ? 'alert-success' : 'alert-warning'} mb-0`} role="status">
      <div className="d-flex gap-2">
        <i className={`fas ${result.success ? 'fa-check-circle' : 'fa-exclamation-triangle'} mt-1`} aria-hidden="true" />
        <span>{result.message}</span>
      </div>
      {result.readable.length > 0 && (
        <div className="small mt-2 connection-test__readable">
          <span className="me-1">Readable through this connection:</span>
          {result.readable.map((r, i) => (
            <span key={r.schema}>
              {i > 0 && ', '}
              <code>{r.schema}</code> ({r.tables} {r.tables === 1 ? 'table' : 'tables'})
            </span>
          ))}
        </div>
      )}
      {result.tables.length > 0 && (
        <ul className="list-unstyled mb-0 mt-2 small connection-test__tables">
          {result.tables.map((t) => (
            <li key={`${t.kind}:${t.table}`}>
              <i className={`fas ${t.readable ? 'fa-check text-success' : 'fa-times text-danger'} me-2`} aria-hidden="true" />
              <span className="text-capitalize">{t.kind}</span> {t.name} <code>{t.table}</code>
              {t.error && <span className="d-block ms-4 text-danger">{t.error}</span>}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
