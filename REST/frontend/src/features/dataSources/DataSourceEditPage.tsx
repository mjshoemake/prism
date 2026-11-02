import { useEffect, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams } from 'react-router';
import { isAbortError, isConflict, serverMessage } from '../../api/client';
import { dataSourcesApi } from '../../api/dataSourcesApi';
import type {
  ConnectionTestResult,
  DataSource,
  DataSourceProperty,
  DataSourceRequest,
  DbConnectionMethod,
} from '../../api/types';
import { useAuth } from '../../auth/useAuth';
import { ErrorAlert } from '../../components/ErrorAlert';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { useToast } from '../../components/toast/useToast';
import { ConnectionTestPanel } from './components/ConnectionTestPanel';
import { METHODS, isReachable } from './labels';

/** The form's values; numbers stay text until saved so a half-typed port isn't lost. */
interface Form {
  name: string;
  description: string;
  connectionType: 'database' | 'api';
  method: DbConnectionMethod;
  host: string;
  port: string;
  dbName: string;
  dbSecret: string;
  apiUrl: string;
  apiSecret: string;
  /** direct_dev only (TEMPORARY, SQL/024): the login, saved as properties. */
  username: string;
  /** Typed password; blank keeps the saved one. */
  password: string;
  /** Saved settings the form doesn't show (e.g. sslmode set in SQL), sent back unchanged. */
  otherProperties: DataSourceProperty[];
}

const EMPTY: Form = {
  name: '',
  description: '',
  connectionType: 'database',
  method: 'prism_database',
  host: '',
  port: '',
  dbName: '',
  dbSecret: '',
  apiUrl: '',
  apiSecret: '',
  username: '',
  password: '',
  otherProperties: [],
};

/** Properties the form edits itself; anything else saved is kept as it is. */
const LOGIN_PROPERTIES = new Set(['username', 'password']);

/**
 * New / Edit data source (/#/data-sources/new, /#/data-sources/{pk}/edit): name, description and the connection —
 * a database (how Prism reaches it, and for client databases host, port, database name and the NAME of the secret
 * holding the credentials) or a client-run API. A saved source can be tested; the Prism database and the temporary
 * development method can be reached so far, other connections are kept for later.
 * <p>
 * TEMPORARY (owner, 2026-11-01; SQL/024): "Direct (development only)" takes a username and password, saved in Prism's
 * own database. The password is write-only: never shown again, blank keeps the saved one. Offered only while the
 * server allows it (prism.data-sources.allow-dev-connections). Remove with the method once the secrets store exists.
 */
export function DataSourceEditPage() {
  const { enterprisePk } = useAuth();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const params = useParams();
  const pk = params.dataSourcePk === undefined ? null : Number(params.dataSourcePk);
  const isNew = pk === null;

  const [form, setForm] = useState<Form>(EMPTY);
  const [original, setOriginal] = useState<DataSource | null>(null);
  const [loading, setLoading] = useState(!isNew);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [nameError, setNameError] = useState<string | null>(null);
  /** A missing username or password (direct_dev), shown under that field. */
  const [loginError, setLoginError] = useState<{ field: 'username' | 'password'; message: string } | null>(null);
  const [saving, setSaving] = useState(false);
  const [test, setTest] = useState<ConnectionTestResult | 'running' | null>(null);
  const [devAllowed, setDevAllowed] = useState(false);

  useEffect(() => {
    const controller = new AbortController();
    dataSourcesApi
      .options(controller.signal)
      .then((o) => setDevAllowed(o.dev_connections_allowed))
      .catch(() => setDevAllowed(false));
    return () => controller.abort();
  }, []);

  useEffect(() => {
    if (isNew || enterprisePk === null) return;
    if (pk === null || Number.isNaN(pk)) {
      setLoadError('Data source not found.');
      setLoading(false);
      return;
    }
    const controller = new AbortController();
    dataSourcesApi
      .get(enterprisePk, pk, controller.signal)
      .then((ds) => {
        setOriginal(ds);
        setForm(formOf(ds));
      })
      .catch((e) => {
        if (!isAbortError(e)) setLoadError(serverMessage(e));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [enterprisePk, isNew, pk]);

  const set = (change: Partial<Form>) => setForm((f) => ({ ...f, ...change }));
  const external = form.connectionType === 'database' && form.method !== 'prism_database';
  const devConnection = form.connectionType === 'database' && form.method === 'direct_dev';
  const passwordSaved =
    original?.db_connection_method === 'direct_dev' &&
    original.properties.some((p) => p.name === 'password' && p.has_value);
  // The development method is listed only while the server allows it, or when this source already uses it.
  const methods = METHODS.filter(
    (m) => m.value !== 'direct_dev' || devAllowed || original?.db_connection_method === 'direct_dev',
  );

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    if (enterprisePk === null) return;
    setSaveError(null);
    if (!form.name.trim()) {
      setNameError('A name is required.');
      return;
    }
    const port = form.port.trim() === '' ? null : Number(form.port);
    if (port !== null && (!Number.isInteger(port) || port < 1 || port > 65535)) {
      setSaveError('The port must be a whole number between 1 and 65535.');
      return;
    }
    if (devConnection && !form.username.trim()) {
      setLoginError({ field: 'username', message: 'A username is required (a read-only login, e.g. prism_reader).' });
      document.getElementById('dsUsername')?.focus();
      return;
    }
    if (devConnection && !form.password && !passwordSaved) {
      setLoginError({ field: 'password', message: 'A password is required.' });
      document.getElementById('dsPassword')?.focus();
      return;
    }
    const request = toRequest(form, port);
    setSaving(true);
    try {
      const saved = pk === null
        ? await dataSourcesApi.create(enterprisePk, request)
        : await dataSourcesApi.update(enterprisePk, pk, request);
      showToast(`${isNew ? 'Created' : 'Saved'} data source: ${saved.name}`, 'success');
      navigate('/data-sources');
    } catch (err) {
      if (isConflict(err)) setNameError(serverMessage(err));
      else setSaveError(`Not saved: ${serverMessage(err)}`);
    } finally {
      setSaving(false);
    }
  };

  const runTest = async () => {
    if (enterprisePk === null || pk === null) return;
    setTest('running');
    try {
      setTest(await dataSourcesApi.test(enterprisePk, pk));
    } catch (err) {
      setTest({ success: false, message: serverMessage(err), readable: [], tables: [] });
    }
  };

  if (enterprisePk === null) {
    return (
      <div className="container py-3">
        <ErrorAlert message="Your session has no enterprise. Sign out and sign in again." />
      </div>
    );
  }
  if (loading) return <LoadingSpinner caption="Loading data source..." />;
  if (loadError) {
    return (
      <div className="container py-3">
        <ErrorAlert message={loadError} />
        <Link to="/data-sources">Back to data sources</Link>
      </div>
    );
  }

  const methodHint = METHODS.find((m) => m.value === form.method)?.hint;

  return (
    <div className="container py-3 data-source-edit">
      <form className="card shadow-purple page-card" onSubmit={(e) => void submit(e)} noValidate>
        <div className="card-header">
          <h5 className="mb-0">{isNew ? 'New data source' : `Edit data source: ${original?.name ?? ''}`}</h5>
        </div>
        <div className="card-body">
          {saveError && <ErrorAlert message={saveError} />}

          <div className="mb-3 data-source-edit__name">
            <label className="form-label" htmlFor="dsName">Name</label>
            <input
              id="dsName"
              className={`form-control ${nameError ? 'is-invalid' : ''}`}
              value={form.name}
              maxLength={60}
              onChange={(e) => {
                set({ name: e.target.value });
                setNameError(null);
              }}
              aria-describedby={nameError ? 'dsNameError' : undefined}
            />
            {nameError && <div id="dsNameError" className="invalid-feedback">{nameError}</div>}
          </div>

          <div className="mb-3">
            <label className="form-label" htmlFor="dsDescription">Description <span className="text-muted">(optional)</span></label>
            <textarea
              id="dsDescription"
              className="form-control"
              rows={2}
              maxLength={2000}
              value={form.description}
              onChange={(e) => set({ description: e.target.value })}
            />
          </div>

          <fieldset className="mb-3">
            <legend className="form-label fs-6">Connection</legend>
            <div className="btn-group btn-group-sm" role="radiogroup" aria-label="Connection">
              <input type="radio" className="btn-check" name="connectionType" id="connDatabase"
                checked={form.connectionType === 'database'} onChange={() => set({ connectionType: 'database' })} />
              <label className="btn btn-outline-secondary" htmlFor="connDatabase">
                <i className="fas fa-database me-1" aria-hidden="true" />Database
              </label>
              <input type="radio" className="btn-check" name="connectionType" id="connApi"
                checked={form.connectionType === 'api'} onChange={() => set({ connectionType: 'api' })} />
              <label className="btn btn-outline-secondary" htmlFor="connApi">
                <i className="fas fa-cloud me-1" aria-hidden="true" />Client API
              </label>
            </div>
          </fieldset>

          {form.connectionType === 'database' ? (
            <fieldset className="group-box mb-3">
              <legend className="group-box__legend">Database</legend>
              <div className="mb-2">
                <label className="form-label" htmlFor="dsMethod">How Prism connects</label>
                <select id="dsMethod" className="form-select form-select-sm" value={form.method}
                  onChange={(e) => set({ method: e.target.value as DbConnectionMethod })}>
                  {methods.map((m) => <option key={m.value} value={m.value}>{m.label}</option>)}
                </select>
                {methodHint && <div className="form-text text-muted">{methodHint}</div>}
              </div>
              {external && (
                <>
                  <div className="row g-2 mb-2">
                    <div className="col-sm-8">
                      <label className="form-label" htmlFor="dsHost">Host</label>
                      <input id="dsHost" className="form-control form-control-sm" placeholder="db.example.com"
                        value={form.host} onChange={(e) => set({ host: e.target.value })} />
                    </div>
                    <div className="col-sm-4">
                      <label className="form-label" htmlFor="dsPort">Port <span className="text-muted">(optional)</span></label>
                      <input id="dsPort" className="form-control form-control-sm" inputMode="numeric" placeholder="5432"
                        value={form.port} onChange={(e) => set({ port: e.target.value })} />
                    </div>
                  </div>
                  <div className="mb-2">
                    <label className="form-label" htmlFor="dsDbName">Database name</label>
                    <input id="dsDbName" className="form-control form-control-sm" value={form.dbName}
                      onChange={(e) => set({ dbName: e.target.value })} />
                  </div>
                  {devConnection ? (
                    <DevLogin
                      username={form.username}
                      password={form.password}
                      passwordSaved={passwordSaved}
                      error={loginError}
                      onChange={(change) => {
                        set(change);
                        setLoginError(null);
                      }}
                    />
                  ) : (
                    <SecretField id="dsDbSecret" value={form.dbSecret} onChange={(v) => set({ dbSecret: v })} />
                  )}
                </>
              )}
            </fieldset>
          ) : (
            <fieldset className="group-box mb-3">
              <legend className="group-box__legend">Client API</legend>
              <div className="mb-2">
                <label className="form-label" htmlFor="dsApiUrl">URL</label>
                <input id="dsApiUrl" className="form-control form-control-sm" placeholder="https://metrics.example.com/prism"
                  value={form.apiUrl} onChange={(e) => set({ apiUrl: e.target.value })} />
              </div>
              <SecretField id="dsApiSecret" value={form.apiSecret} onChange={(v) => set({ apiSecret: v })} />
            </fieldset>
          )}

          {!isReachable({ connection_type: form.connectionType, db_connection_method: form.method }) && (
            <div className="alert alert-info small mb-3" role="note">
              <i className="fas fa-info-circle me-2" aria-hidden="true" />
              Prism can't connect to this kind of data source yet (a later roadmap step). The settings are saved so it's
              ready when it can.
            </div>
          )}

          {!isNew && (
            <div className="mb-1">
              <button type="button" className="btn btn-sm btn-outline-secondary" onClick={() => void runTest()}
                disabled={test === 'running'}>
                <i className="fas fa-plug me-1" aria-hidden="true" />
                {test === 'running' ? 'Testing...' : 'Test connection'}
              </button>
              <span className="form-text text-muted ms-2">Tests the saved settings.</span>
              {test && test !== 'running' && <div className="mt-2"><ConnectionTestPanel result={test} /></div>}
            </div>
          )}
        </div>
        <div className="card-footer d-flex gap-2 justify-content-end">
          <Link className="btn btn-outline-secondary" to="/data-sources">Cancel</Link>
          <button type="submit" className="btn btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Save'}</button>
        </div>
      </form>
    </div>
  );
}

/** TEMPORARY (SQL/024): the development-only login. The password is write-only. */
function DevLogin({
  username,
  password,
  passwordSaved,
  error,
  onChange,
}: {
  username: string;
  password: string;
  passwordSaved: boolean;
  error: { field: 'username' | 'password'; message: string } | null;
  onChange: (change: Partial<Form>) => void;
}) {
  return (
    <>
      <div className="alert alert-warning small mb-2 dev-connection-warning" role="note">
        <i className="fas fa-exclamation-triangle me-2" aria-hidden="true" />
        Development only: this login is saved in Prism’s own database, not a secrets store. Use a read-only login
        (e.g. prism_reader) on a development database, never a client’s.
      </div>
      <div className="row g-2 mb-1">
        <div className="col-sm-6">
          <label className="form-label" htmlFor="dsUsername">Username</label>
          {/* No placeholder: a grey "prism_reader" looked like a value already typed in (owner, 2026-11-01). */}
          <input id="dsUsername"
            className={`form-control form-control-sm ${error?.field === 'username' ? 'is-invalid' : ''}`}
            autoComplete="off" value={username} onChange={(e) => onChange({ username: e.target.value })}
            aria-describedby="dsUsernameHelp" />
          {error?.field === 'username'
            ? <div id="dsUsernameHelp" className="invalid-feedback">{error.message}</div>
            : <div id="dsUsernameHelp" className="form-text text-muted">A read-only login, e.g. prism_reader.</div>}
        </div>
        <div className="col-sm-6">
          <label className="form-label" htmlFor="dsPassword">Password</label>
          <input id="dsPassword" type="password"
            className={`form-control form-control-sm ${error?.field === 'password' ? 'is-invalid' : ''}`}
            autoComplete="new-password"
            placeholder={passwordSaved ? 'Saved (leave blank to keep)' : ''}
            value={password} onChange={(e) => onChange({ password: e.target.value })}
            aria-describedby="dsPasswordHelp" />
          {error?.field === 'password'
            ? <div id="dsPasswordHelp" className="invalid-feedback">{error.message}</div>
            : (
              <div id="dsPasswordHelp" className="form-text text-muted">
                {passwordSaved ? 'Never shown again. Type a new one to replace it.' : 'Never shown again once saved.'}
              </div>
            )}
        </div>
      </div>
    </>
  );
}

function SecretField({ id, value, onChange }: { id: string; value: string; onChange: (value: string) => void }) {
  return (
    <div className="mb-1">
      <label className="form-label" htmlFor={id}>Secret name</label>
      <input id={id} className="form-control form-control-sm" placeholder="prism/acme/warehouse-reader" autoComplete="off"
        value={value} onChange={(e) => onChange(e.target.value)} />
      <div className="form-text text-muted">
        The name of the credentials in the secrets store. Never the password itself: Prism doesn't store credentials.
      </div>
    </div>
  );
}

function formOf(ds: DataSource): Form {
  return {
    name: ds.name,
    description: ds.description ?? '',
    connectionType: ds.connection_type,
    method: ds.db_connection_method ?? 'prism_database',
    host: ds.db_host ?? '',
    port: ds.db_port === null ? '' : String(ds.db_port),
    dbName: ds.db_name ?? '',
    dbSecret: ds.db_credentials_ref ?? '',
    apiUrl: ds.api_url ?? '',
    apiSecret: ds.api_credentials_ref ?? '',
    username: ds.properties.find((p) => p.name === 'username')?.value ?? '',
    password: '',
    otherProperties: ds.properties.filter((p) => !LOGIN_PROPERTIES.has(p.name) && !p.secret),
  };
}

/** The request for the API; the server ignores (and clears) whatever doesn't apply to the chosen connection. */
export function toRequest(form: Form, port: number | null): DataSourceRequest {
  const text = (v: string) => (v.trim() === '' ? null : v.trim());
  return {
    name: form.name.trim(),
    description: text(form.description),
    connection_type: form.connectionType,
    db_connection_method: form.connectionType === 'database' ? form.method : null,
    db_host: text(form.host),
    db_port: port,
    db_name: text(form.dbName),
    db_credentials_ref: text(form.dbSecret),
    api_url: text(form.apiUrl),
    api_credentials_ref: text(form.apiSecret),
    // Only the development method has properties; the server clears them for the others.
    properties:
      form.connectionType === 'database' && form.method === 'direct_dev'
        ? [
            { name: 'username', value: form.username.trim() },
            // null keeps the saved password; the password is sent as typed (spaces are allowed).
            { name: 'password', value: form.password === '' ? null : form.password },
            ...form.otherProperties.map((p) => ({ name: p.name, value: p.value })),
          ]
        : [],
  };
}
