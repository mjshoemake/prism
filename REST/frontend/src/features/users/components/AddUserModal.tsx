import { useState, type FormEvent } from 'react';
import { Modal } from '../../../components/Modal';
import type { ActionOutcome } from '../useUsers';
import {
  DEFAULT_TEMPORARY_PASSWORD,
  temporaryPasswordValue,
  validateTemporaryPassword,
  type TemporaryPasswordChoice,
} from '../temporaryPassword';
import { TemporaryPasswordFields } from './TemporaryPasswordFields';

interface AddUserModalProps {
  onCreate: (email: string, username: string, temporaryPassword: string | null) => Promise<ActionOutcome>;
  onClose: () => void;
}

const FORM_ID = 'add-user-form';

export function AddUserModal({ onCreate, onClose }: AddUserModalProps) {
  const [email, setEmail] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState<TemporaryPasswordChoice>(DEFAULT_TEMPORARY_PASSWORD);
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const problem = !email.trim() ? 'Email is required.' : validateTemporaryPassword(password);
    if (problem) {
      setMessage(problem);
      return;
    }
    setSaving(true);
    setMessage(null);
    const outcome = await onCreate(email.trim(), username.trim(), temporaryPasswordValue(password));
    if (outcome.ok) {
      onClose();
    } else {
      setMessage(outcome.message ?? 'The user could not be created.');
      setSaving(false);
    }
  };

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
          />
        </div>
        <TemporaryPasswordFields idPrefix="add-user-temp" value={password} onChange={setPassword} disabled={saving} />
      </form>
    </Modal>
  );
}
