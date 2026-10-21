import { useState, type FormEvent } from 'react';
import type { User } from '../../../api/types';
import { Modal } from '../../../components/Modal';
import type { TemporaryPasswordOutcome } from '../useUsers';
import { TemporaryPasswordNotice } from './TemporaryPasswordNotice';

interface AddUserModalProps {
  onCreate: (email: string, username: string | null) => Promise<TemporaryPasswordOutcome>;
  onClose: () => void;
}

const FORM_ID = 'add-user-form';

/**
 * Add user. The server generates the temporary password; after creating, the modal shows it once
 * (with a Copy button) so the administrator can pass it on.
 */
export function AddUserModal({ onCreate, onClose }: AddUserModalProps) {
  const [email, setEmail] = useState('');
  const [username, setUsername] = useState('');
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [created, setCreated] = useState<{ user: User | undefined; password: string } | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!email.trim()) {
      setMessage('Email is required.');
      return;
    }
    setSaving(true);
    setMessage(null);
    const outcome = await onCreate(email.trim(), username.trim() || null);
    setSaving(false);
    if (outcome.ok && outcome.temporaryPassword) {
      setCreated({ user: outcome.user, password: outcome.temporaryPassword });
    } else if (outcome.ok) {
      onClose();
    } else {
      setMessage(outcome.message ?? 'The user could not be created.');
    }
  };

  if (created) {
    return (
      <Modal
        title="User created"
        onClose={onClose}
        footer={
          <button type="button" className="btn btn-primary" onClick={onClose}>
            Done
          </button>
        }
      >
        <TemporaryPasswordNotice
          idPrefix="add-user-temp"
          password={created.password}
          expiresAt={created.user?.temp_password_expires_at}
          recipient={created.user?.email ?? email.trim()}
        />
      </Modal>
    );
  }

  return (
    <Modal
      title="Add user"
      onClose={onClose}
      footer={
        <>
          <button type="button" className="btn btn-outline-secondary" onClick={onClose} disabled={saving}>
            Cancel
          </button>
          <button type="submit" form={FORM_ID} className="btn btn-primary" disabled={saving}>
            {saving ? 'Adding…' : 'Add user'}
          </button>
        </>
      }
    >
      {message && (
        <div className="alert alert-warning" role="alert">
          {message}
        </div>
      )}
      <form id={FORM_ID} onSubmit={handleSubmit} noValidate>
        <div className="mb-3">
          <label className="form-label" htmlFor="add-user-email">
            Email
          </label>
          <input
            id="add-user-email"
            className="form-control"
            type="email"
            autoComplete="off"
            required
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            disabled={saving}
          />
        </div>
        <div className="mb-3">
          <label className="form-label" htmlFor="add-user-username">
            Username <span className="text-muted">(optional)</span>
          </label>
          <input
            id="add-user-username"
            className="form-control"
            type="text"
            autoComplete="off"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            disabled={saving}
          />
        </div>
        <small className="form-text text-muted">
          A temporary password is generated and shown after the user is added. The user must choose their own
          password when they first sign in.
        </small>
      </form>
    </Modal>
  );
}
