import { Fragment, useEffect, useMemo, useState, type FormEvent } from 'react';
import { Link, useNavigate, useParams, useSearchParams } from 'react-router';
import { dashboardsApi } from '../../api/dashboardsApi';
import { isAbortError } from '../../api/client';
import type { Dashboard, DashboardChart, DashboardLayout, DashboardRequest, MetricInfo, YAxisMode } from '../../api/types';
import { useAuth } from '../../auth/useAuth';
import { ErrorAlert } from '../../components/ErrorAlert';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { useToast } from '../../components/toast/useToast';
import { useMetricList } from '../metrics/useMetricList';
import { LAYOUTS, areaLabel, visiblePositions } from './layouts';
import { isConflict, serverMessage } from './useDashboards';

const MAX_NAME = 60;
const ALL_POSITIONS = [1, 2, 3, 4] as const;

/** One chart area as edited: bounds stay text until saved, so a half-typed number isn't lost. */
export interface AreaForm {
  metricPk: number | null;
  mode: YAxisMode;
  min: string;
  max: string;
}

const EMPTY_AREA: AreaForm = { metricPk: null, mode: 'metric', min: '', max: '' };

/**
 * New / Edit dashboard (/#/dashboards/new, /#/dashboards/{pk}/edit): name, layout, and per visible chart area its
 * metric and y-axis scale. Areas hidden by a smaller layout keep their settings and are saved with the dashboard.
 * Save and Cancel go back where the user came from: Analysis (`?from=analysis`, same dashboard) or the list.
 */
export function DashboardEditPage() {
  const { userId } = useAuth();
  const navigate = useNavigate();
  const { showToast } = useToast();
  const params = useParams();
  const [searchParams] = useSearchParams();
  const metricList = useMetricList();

  const dashboardPk = params.dashboardPk === undefined ? null : Number(params.dashboardPk);
  const isNew = dashboardPk === null;
  const fromAnalysis = searchParams.get('from') === 'analysis';

  const [name, setName] = useState('');
  const [layout, setLayout] = useState<DashboardLayout>(1);
  const [areas, setAreas] = useState<Record<number, AreaForm>>(() => emptyAreas());
  const [original, setOriginal] = useState<Dashboard | null>(null);
  const [loading, setLoading] = useState(!isNew);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [nameError, setNameError] = useState<string | null>(null);
  const [submitted, setSubmitted] = useState(false);
  const [saving, setSaving] = useState(false);

  // Edit: load the dashboard. New: start with one chart showing the first metric.
  useEffect(() => {
    if (isNew || !userId || dashboardPk === null || Number.isNaN(dashboardPk)) {
      if (!isNew) {
        setLoadError('Dashboard not found.');
        setLoading(false);
      }
      return;
    }
    const controller = new AbortController();
    dashboardsApi
      .get(userId, dashboardPk, controller.signal)
      .then((d) => {
        setOriginal(d);
        setName(d.name);
        setLayout(d.layout);
        setAreas(areasOf(d));
      })
      .catch((e) => {
        if (!isAbortError(e)) setLoadError(serverMessage(e));
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [dashboardPk, isNew, userId]);

  const firstMetricPk = metricList.metrics[0]?.metric_pk ?? null;
  useEffect(() => {
    if (isNew && firstMetricPk !== null) {
      setAreas((current) => (current[1].metricPk === null ? { ...current, 1: { ...current[1], metricPk: firstMetricPk } } : current));
    }
  }, [firstMetricPk, isNew]);

  const positions = visiblePositions(layout);
  const errors = useMemo(() => Object.fromEntries(ALL_POSITIONS.map((p) => [p, areaError(areas[p])])), [areas]);

  const backTo = (pk: number | null) =>
    fromAnalysis ? (pk === null ? '/analysis' : `/analysis?dashboard=${pk}`) : '/dashboards';

  const setArea = (position: number, change: Partial<AreaForm>) =>
    setAreas((current) => ({ ...current, [position]: { ...current[position], ...change } }));

  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setSubmitted(true);
    setSaveError(null);
    const trimmed = name.trim();
    const nameProblem = !trimmed ? 'A name is required.' : trimmed.length > MAX_NAME ? `At most ${MAX_NAME} characters.` : null;
    setNameError(nameProblem);
    if (nameProblem || positions.some((p) => errors[p] !== null) || !userId) return;

    const request: DashboardRequest = { name: trimmed, layout, charts: chartsToSave(areas, positions, original) };
    setSaving(true);
    try {
      const saved =
        dashboardPk === null
          ? await dashboardsApi.create(userId, request)
          : await dashboardsApi.update(userId, dashboardPk, request);
      showToast(`${isNew ? 'Created' : 'Saved'} dashboard: ${saved.name}`, 'success');
      navigate(backTo(saved.dashboard_pk));
    } catch (err) {
      if (isConflict(err)) setNameError(serverMessage(err));
      else setSaveError(`Not saved: ${serverMessage(err)}`);
    } finally {
      setSaving(false);
    }
  };

  if (loading || metricList.loading) return <LoadingSpinner caption="Loading dashboard..." />;
  if (loadError) {
    return (
      <div className="container py-3">
        <ErrorAlert message={loadError} />
        <Link to={backTo(null)}>Back</Link>
      </div>
    );
  }

  return (
    <div className="container py-3 dashboard-edit">
      <form className="card shadow-purple page-card" onSubmit={(e) => void submit(e)} noValidate>
        <div className="card-header">
          <h5 className="mb-0">{isNew ? 'New dashboard' : `Edit dashboard: ${original?.name ?? ''}`}</h5>
        </div>
        <div className="card-body">
          {saveError && <ErrorAlert message={saveError} />}

          <div className="mb-3 dashboard-edit__name">
            <label className="form-label" htmlFor="dashboardName">
              Name
            </label>
            <input
              id="dashboardName"
              className={`form-control ${nameError ? 'is-invalid' : ''}`}
              value={name}
              maxLength={MAX_NAME}
              onChange={(e) => {
                setName(e.target.value);
                setNameError(null);
              }}
              aria-describedby={nameError ? 'dashboardNameError' : undefined}
            />
            {nameError && (
              <div id="dashboardNameError" className="invalid-feedback">
                {nameError}
              </div>
            )}
          </div>

          <fieldset className="mb-3">
            <legend className="form-label fs-6">Layout</legend>
            <div className="btn-group btn-group-sm" role="radiogroup" aria-label="Layout">
              {LAYOUTS.map((option) => (
                <Fragment key={option.layout}>
                  <input
                    type="radio"
                    className="btn-check"
                    name="layout"
                    id={`layout${option.layout}`}
                    checked={layout === option.layout}
                    onChange={() => setLayout(option.layout)}
                  />
                  <label className="btn btn-outline-secondary" htmlFor={`layout${option.layout}`}>
                    <i className={`fas ${option.icon} me-1`} aria-hidden="true" />
                    {option.label}
                  </label>
                </Fragment>
              ))}
            </div>
            <div className="form-text text-muted">Charts hidden by a smaller layout keep their settings and come back with a larger one.</div>
          </fieldset>

          <div className={`dashboard-edit__areas dashboard-edit__areas--${layout}`}>
            {positions.map((position) => (
              <AreaFields
                key={position}
                position={position}
                label={areaLabel(layout, position)}
                area={areas[position]}
                metrics={metricList.metrics}
                error={submitted ? errors[position] : null}
                onChange={(change) => setArea(position, change)}
              />
            ))}
          </div>
        </div>
        <div className="card-footer d-flex gap-2 justify-content-end">
          <Link className="btn btn-outline-secondary" to={backTo(dashboardPk)}>
            Cancel
          </Link>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? 'Saving...' : 'Save'}
          </button>
        </div>
      </form>
    </div>
  );
}

interface AreaFieldsProps {
  position: number;
  label: string;
  area: AreaForm;
  metrics: readonly MetricInfo[];
  error: string | null;
  onChange: (change: Partial<AreaForm>) => void;
}

/** The group box of one chart area: its metric and y-axis scale (custom: minimum and/or maximum). */
function AreaFields({ position, label, area, metrics, error, onChange }: AreaFieldsProps) {
  const metric = metrics.find((m) => m.metric_pk === area.metricPk) ?? null;
  const id = (field: string) => `area${position}${field}`;

  return (
    <fieldset className="dashboard-area" data-position={position}>
      <legend className="dashboard-area__legend">{label}</legend>
      <div className="mb-2">
        <label className="form-label" htmlFor={id('Metric')}>
          Metric
        </label>
        <select
          id={id('Metric')}
          className="form-select form-select-sm"
          value={area.metricPk ?? ''}
          onChange={(e) => onChange({ metricPk: e.target.value === '' ? null : Number(e.target.value) })}
        >
          <option value="">— No metric —</option>
          {metrics.map((m) => (
            <option key={m.metric_pk} value={m.metric_pk}>
              {m.title}
            </option>
          ))}
        </select>
      </div>
      <div className="mb-2">
        <label className="form-label" htmlFor={id('Scale')}>
          Y-axis scale
        </label>
        <select
          id={id('Scale')}
          className="form-select form-select-sm"
          value={area.mode}
          disabled={!metric}
          onChange={(e) => onChange({ mode: e.target.value as YAxisMode })}
        >
          <option value="metric">Metric default{metric ? ` (${metricDefaultText(metric)})` : ''}</option>
          <option value="zero_100">0 to 100</option>
          <option value="fit">Fit to data</option>
          <option value="custom">Custom</option>
        </select>
      </div>
      {metric && area.mode === 'custom' && (
        <>
          <div className="d-flex gap-2">
            <div className="flex-fill">
              <label className="form-label" htmlFor={id('Min')}>
                Minimum
              </label>
              <input
                id={id('Min')}
                type="number"
                step="any"
                className={`form-control form-control-sm ${error ? 'is-invalid' : ''}`}
                placeholder="0"
                value={area.min}
                onChange={(e) => onChange({ min: e.target.value })}
              />
            </div>
            <div className="flex-fill">
              <label className="form-label" htmlFor={id('Max')}>
                Maximum
              </label>
              <input
                id={id('Max')}
                type="number"
                step="any"
                className={`form-control form-control-sm ${error ? 'is-invalid' : ''}`}
                placeholder="Fit to data"
                value={area.max}
                onChange={(e) => onChange({ max: e.target.value })}
              />
            </div>
          </div>
          {error ? (
            <div className="invalid-feedback d-block">{error}</div>
          ) : (
            <div className="form-text text-muted">
              Bars above the maximum are drawn to the top with a striped end, and show their real value on hover.
            </div>
          )}
        </>
      )}
    </fieldset>
  );
}

/** "0 to 100", "fit to data" or "fixed 20 to 80": what a metric's own default scale is. */
export function metricDefaultText(metric: MetricInfo): string {
  switch (metric.scale_mode) {
    case 'zero_to_100':
      return '0 to 100';
    case 'fixed':
      return `fixed ${metric.scale_min ?? 0} to ${metric.scale_max ?? 'fit'}`;
    default:
      return 'fit to data';
  }
}

function emptyAreas(): Record<number, AreaForm> {
  return Object.fromEntries(ALL_POSITIONS.map((p) => [p, EMPTY_AREA]));
}

/** The form state of a saved dashboard's four chart areas. */
export function areasOf(dashboard: Dashboard): Record<number, AreaForm> {
  const areas = emptyAreas();
  for (const c of dashboard.charts) {
    areas[c.position] = {
      metricPk: c.metric_pk,
      mode: c.y_axis_mode ?? 'metric',
      min: c.y_axis_min === null || c.y_axis_min === undefined ? '' : String(c.y_axis_min),
      max: c.y_axis_max === null || c.y_axis_max === undefined ? '' : String(c.y_axis_max),
    };
  }
  return areas;
}

/** What is wrong with an area's custom scale (mirrors the server, SQL/021); null when nothing. */
export function areaError(area: AreaForm): string | null {
  if (area.metricPk === null || area.mode !== 'custom') return null;
  const min = parseBound(area.min);
  const max = parseBound(area.max);
  if (min === 'invalid' || max === 'invalid') return 'Enter numbers only.';
  if (min === null && max === null) return 'Enter a minimum, a maximum or both.';
  if (min !== null && max !== null && min >= max) return 'The minimum must be less than the maximum.';
  return null;
}

function parseBound(text: string): number | null | 'invalid' {
  if (text.trim() === '') return null;
  const n = Number(text);
  return Number.isFinite(n) ? n : 'invalid';
}

/** An area as saved, or null when it has no metric. Bounds are sent only for a custom scale. */
export function toChart(position: number, area: AreaForm): DashboardChart | null {
  if (area.metricPk === null) return null;
  const custom = area.mode === 'custom';
  const min = custom ? parseBound(area.min) : null;
  const max = custom ? parseBound(area.max) : null;
  return {
    position,
    metric_pk: area.metricPk,
    y_axis_mode: area.mode,
    y_axis_min: typeof min === 'number' ? min : null,
    y_axis_max: typeof max === 'number' ? max : null,
  };
}

/**
 * The chart areas to save: every visible area with a metric, plus the hidden ones (kept for a larger layout). A
 * hidden area whose scale can't be saved as edited keeps what was saved before.
 */
export function chartsToSave(
  areas: Record<number, AreaForm>,
  visible: readonly number[],
  original: Dashboard | null,
): DashboardChart[] {
  const charts: DashboardChart[] = [];
  for (const position of ALL_POSITIONS) {
    const area = areas[position];
    if (visible.includes(position) || areaError(area) === null) {
      const chart = toChart(position, area);
      if (chart) charts.push(chart);
    } else {
      const saved = original?.charts.find((c) => c.position === position);
      if (saved) charts.push(saved);
    }
  }
  return charts;
}
