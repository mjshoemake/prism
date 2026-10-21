import { useEffect, useState, type FormEvent } from 'react';
import { useAuth } from '../../auth/useAuth';
import { ErrorAlert } from '../../components/ErrorAlert';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { formatDateTime } from '../users/format';
import { useSystemSettings } from './useSystemSettings';
import { MAX_VALID_HOURS, MIN_VALID_HOURS, validateValidHours } from './validHours';

const FORM_ID = 'system-settings-form';

/**
 * Admin → System Settings for the signed-in user's enterprise. Only Save and Cancel:
 * Save stores the edited values, Cancel puts back the saved ones.
 */
export function SystemSettingsPage() {
  const { enterprisePk } = useAuth();
  const { settings, loading, error, save } = useSystemSettings(enterprisePk);

  const savedHours = settings ? String(settings.temp_password_valid_hours) : '';
  const [hours, setHours] = useState('');
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  // Show the saved value whenever it (re)loads.
  useEffect(() => setHours(savedHours), [savedHours]);

  const dirty = settings !== null && hours !== savedHours;

  const handleCancel = () => {
    setHours(savedHours);
    setMessage(null);
  };

  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    const problem = validateValidHours(hours);
    if (problem) {
      setMessage(problem);
      return;
    }
    setSaving(true);
    setMessage(null);
    const outcome = await save(Number(hours));
    setSaving(false);
    if (!outcome.ok) setMessage(outcome.message ?? 'The settings could not be saved.');
  };

  if (enterprisePk === null) {
    return (
      <div className="container py-3">
        <ErrorAlert message="Your session has no enterprise. Sign out and sign in again." />
      </div>
    );
  }

  const renderBody = () => {
    if (loading && !settings) return <LoadingSpinner />;
    if (error) return <ErrorAlert message={error} />;
    return (
      <form id={FORM_ID} onSubmit={handleSave} noValidate>
        <section aria-labelledby="temp-password-heading">
          <h6 id="temp-password-heading">Temporary passwords</h6>
          <p className="small text-muted">
            New users and password resets get a randomly generated temporary password, shown once to the
            administrator. It stops working after this many hours if the user hasn't signed in and chosen their
            own password; an administrator then has to reset it.
          </p>

          {message && (
            <div className="alert alert-warning" role="alert">
              {message}
            </div>
          )}

          <label className="form-label" htmlFor="settings-temp-password-hours">
            Valid for (hours)
          </label>
          <input
            id="settings-temp-password-hours"
            className="form-control settings-number"
            type="number"
            inputMode="numeric"
            min={MIN_VALID_HOURS}
            max={MAX_VALID_HOURS}
            step={1}
            value={hours}
            onChange={(e) => setHours(e.target.value)}
            disabled={saving}
          />
          <small className="form-text text-muted">
            {MIN_VALID_HOURS}–{MAX_VALID_HOURS}. Applies to temporary passwords issued after saving.
          </small>
        </section>
        {settings?.updated_at && (
          <p className="small text-muted mt-3 mb-0">Last changed {formatDateTime(settings.updated_at)}.</p>
        )}
      </form>
    );
  };

  return (
    <div className="container py-4 narrow-container">
      <div className="card shadow-purple">
        <div className="card-header">
          <h5 className="mb-0">System Settings</h5>
        </div>
        <div className="card-body">{renderBody()}</div>
        <div className="card-footer d-flex justify-content-end gap-2">
          <button type="button" className="btn btn-outline-secondary" onClick={handleCancel} disabled={saving || !dirty}>
            Cancel
          </button>
          <button type="submit" form={FORM_ID} className="btn btn-primary" disabled={saving || !dirty}>
            {saving ? 'Saving…' : 'Save'}
          </button>
        </div>
      </div>
    </div>
  );
}
