import { useState, type FormEvent } from 'react';
import type { UpdateUserInput, User } from '../../../api/types';
import { Modal } from '../../../components/Modal';
import { formatDateTime, timestampValue } from '../format';
import type { ActionOutcome } from '../useUsers';
import {
  DEFAULT_TEMPORARY_PASSWORD,
  temporaryPasswordValue,
  validateTemporaryPassword,
  type TemporaryPasswordChoice,
} from '../temporaryPassword';
import { TemporaryPasswordFields } from './TemporaryPasswordFields';

interface EditUserModalProps {
  user: User;
  /** True when editing your own account: deactivation is not allowed. */
  isSelf: boolean;
  onSave: (user: User, changes: UpdateUserInput) => Promise<ActionOutcome>;
  onResetPassword: (user: User, temporaryPassword: string | null) => Promise<ActionOutcome>;
  onClose: () => void;
}

const FORM_ID = 'edit-user-form';

export function EditUserModal({ user, isSelf, onSave, onResetPassword, onClose }: EditUserModalProps) {
  const [email, setEmail] = useState(user.email ?? '');
  const [username, setUsername] = useState(user.username ?? '');
  const [isActive, setIsActive] = useState(user.is_active);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const [showReset, setShowReset] = useState(false);
  const [resetChoice, setResetChoice] = useState<TemporaryPasswordChoice>(DEFAULT_TEMPORARY_PASSWORD);
  const [resetting, setResetting] = useState(false);
  const [resetMessage, setResetMessage] = useState<{ ok: boolean; text: string } | null>(null);

  const dirty = email !== (user.email ?? '') || username !== (user.username ?? '') || isActive !== user.is_active;
  const locked = timestampValue(user.locked_until) > Date.now();

  const handleSave = async (e: FormEvent) => {
    e.preventDefault();
    if (!email.trim()) {
      setMessage('Email is required.');
      return;
    }
    setSaving(true);
    setMessage(null);
    const outcome = await onSave(user, { email: email.trim(), username: username.trim() || null, isActive });
    if (outcome.ok) {
      onClose();
    } else {
      setMessage(outcome.message ?? 'The user could not be updated.');
      setSaving(false);
    }
  };

  const handleReset = async () => {
    const problem = validateTemporaryPassword(resetChoice);
    if (problem) {
      setResetMessage({ ok: false, text: problem });
      return;
    }
    setResetting(true);
    setResetMessage(null);
    const outcome = await onResetPassword(user, temporaryPasswordValue(resetChoice));
    setResetting(false);
    if (outcome.ok) {
      setShowReset(false);
      setResetChoice(DEFAULT_TEMPORARY_PASSWORD);
      setResetMessage({ ok: true, text: outcome.message ?? 'Password reset.' });
    } else {
      setResetMessage({ ok: false, text: outcome.message ?? 'The password could not be reset.' });
    }
  };

  const busy = saving || resetting;

  return (
    <Modal
      title={`Edit user — ${user.username || user.email}`}
      onClose={onClose}
      size="lg"
      footer={
        <>
          <button type="button" className="btn btn-outline-secondary" onClick={onClose} disabled={busy}>
            Cancel
          </button>
          <button type="submit" form={FORM_ID} className="btn btn-primary" disabled={busy || !dirty}>
            {saving ? 'Saving…' : 'Save changes'}
          </button>
        </>
      }
    >
      {message && (
        <div className="alert alert-warning" role="alert">
          {message}
        </div>
      )}

      <form id={FORM_ID} onSubmit={handleSave} noValidate>
        <div className="row g-3">
          <div className="col-md-6">
            <label className="form-label" htmlFor="edit-user-email">
              Email
            </label>
            <input
              id="edit-user-email"
              className="form-control"
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              disabled={busy}
            />
          </div>
          <div className="col-md-6">
            <label className="form-label" htmlFor="edit-user-username">
              Username
            </label>
            <input
              id="edit-user-username"
              className="form-control"
              type="text"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={busy}
            />
          </div>
          <div className="col-12">
            <div className="form-check form-switch">
              <input
                id="edit-user-active"
                className="form-check-input"
                type="checkbox"
                role="switch"
                checked={isActive}
                onChange={(e) => setIsActive(e.target.checked)}
                disabled={busy || isSelf}
              />
              <label className="form-check-label" htmlFor="edit-user-active">
                Active
              </label>
            </div>
            <small className="form-text text-muted">
              {isSelf
                ? "You can't deactivate your own account."
                : 'Deactivated users cannot sign in and are hidden from the list by default.'}
            </small>
          </div>
        </div>
      </form>

      <dl className="row small text-muted mt-3 mb-0">
        <dt className="col-sm-3">Created</dt>
        <dd className="col-sm-9">{formatDateTime(user.created_at)}</dd>
        <dt className="col-sm-3">Last sign-in</dt>
        <dd className="col-sm-9">{formatDateTime(user.last_login_at)}</dd>
        {locked && (
          <>
            <dt className="col-sm-3">Locked until</dt>
            <dd className="col-sm-9">{formatDateTime(user.locked_until)} (resetting the password unlocks it)</dd>
          </>
        )}
      </dl>

      <hr />

      <section aria-labelledby="edit-user-password-heading">
        <div className="d-flex justify-content-between align-items-center">
          <h6 id="edit-user-password-heading" className="mb-0">
            Password{' '}
            {user.is_temp_password && <span className="badge text-bg-warning ms-1">Temporary</span>}
          </h6>
          {!showReset && (
            <button
              type="button"
              className="btn btn-sm btn-outline-secondary"
              onClick={() => {
                setShowReset(true);
                setResetMessage(null);
              }}
              disabled={busy}
            >
              Reset password…
            </button>
          )}
        </div>

        {resetMessage && (
          <div className={`alert ${resetMessage.ok ? 'alert-success' : 'alert-warning'} mt-2 mb-0`} role="status">
            {resetMessage.text}
          </div>
        )}

        {showReset && (
          <div className="border rounded p-3 mt-2">
            <TemporaryPasswordFields
              idPrefix="reset-temp"
              value={resetChoice}
              onChange={setResetChoice}
              disabled={resetting}
            />
            <div className="d-flex justify-content-end gap-2 mt-3">
              <button
                type="button"
                className="btn btn-sm btn-outline-secondary"
                onClick={() => setShowReset(false)}
                disabled={resetting}
              >
                Cancel
              </button>
              <button type="button" className="btn btn-sm btn-primary" onClick={handleReset} disabled={busy}>
                {resetting ? 'Resetting…' : 'Reset password'}
              </button>
            </div>
          </div>
        )}
      </section>
    </Modal>
  );
}
