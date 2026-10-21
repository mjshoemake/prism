import { useState, type FormEvent } from 'react';
import type { UpdateUserInput, User } from '../../../api/types';
import { Modal } from '../../../components/Modal';
import { formatDateTime, timestampValue } from '../format';
import { isTemporaryPasswordExpired } from '../temporaryPassword';
import type { ActionOutcome, TemporaryPasswordOutcome } from '../useUsers';
import { TemporaryPasswordNotice } from './TemporaryPasswordNotice';

interface EditUserModalProps {
  user: User;
  /** True when editing your own account: deactivation is not allowed. */
  isSelf: boolean;
  onSave: (user: User, changes: UpdateUserInput) => Promise<ActionOutcome>;
  onResetPassword: (user: User) => Promise<TemporaryPasswordOutcome>;
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
  const [resetting, setResetting] = useState(false);
  const [resetMessage, setResetMessage] = useState<string | null>(null);
  /** Generated password from the last reset, shown once. */
  const [newPassword, setNewPassword] = useState<{ password: string; expiresAt: User['temp_password_expires_at'] } | null>(
    null,
  );

  const dirty = email !== (user.email ?? '') || username !== (user.username ?? '') || isActive !== user.is_active;
  const locked = timestampValue(user.locked_until) > Date.now();
  const tempExpired = isTemporaryPasswordExpired(user);

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
    setResetting(true);
    setResetMessage(null);
    const outcome = await onResetPassword(user);
    setResetting(false);
    if (outcome.ok && outcome.temporaryPassword) {
      setShowReset(false);
      setNewPassword({
        password: outcome.temporaryPassword,
        expiresAt: outcome.user?.temp_password_expires_at ?? null,
      });
    } else {
      setResetMessage(outcome.message ?? 'The password could not be reset.');
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
        {user.is_temp_password && (
          <>
            <dt className="col-sm-3">Temporary password</dt>
            <dd className="col-sm-9">
              {tempExpired ? 'Expired' : 'Valid until'} {formatDateTime(user.temp_password_expires_at)}
              {tempExpired && ' (reset the password to issue a new one)'}
            </dd>
          </>
        )}
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
            {user.is_temp_password &&
              (tempExpired ? (
                <span className="badge text-bg-danger ms-1">Temporary — expired</span>
              ) : (
                <span className="badge text-bg-warning ms-1">Temporary</span>
              ))}
          </h6>
          {!showReset && (
            <button
              type="button"
              className="btn btn-sm btn-outline-secondary"
              onClick={() => {
                setShowReset(true);
                setResetMessage(null);
                setNewPassword(null);
              }}
              disabled={busy}
            >
              Reset password…
            </button>
          )}
        </div>

        {resetMessage && (
          <div className="alert alert-warning mt-2 mb-0" role="alert">
            {resetMessage}
          </div>
        )}

        {newPassword && (
          <div className="mt-2">
            <TemporaryPasswordNotice
              idPrefix="reset-temp"
              password={newPassword.password}
              expiresAt={newPassword.expiresAt}
              recipient={user.email}
            />
          </div>
        )}

        {showReset && (
          <div className="border rounded p-3 mt-2">
            <p className="mb-0">
              Generate a new temporary password for {user.email}? Their current password stops working
              immediately, and any lockout is cleared.
            </p>
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
