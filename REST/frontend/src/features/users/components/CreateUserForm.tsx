import { useState, type FormEvent } from 'react';
import type { CreateUserInput } from '../../../api/types';
import { useToast } from '../../../components/toast/useToast';

interface CreateUserFormProps {
  /** Resolves to true when the user was created (the form is then cleared). */
  onCreate: (input: CreateUserInput) => Promise<boolean>;
}

const EMPTY_FORM = { enterprisePk: '', email: '', username: '' };

export function CreateUserForm({ onCreate }: CreateUserFormProps) {
  const { showToast } = useToast();
  const [form, setForm] = useState(EMPTY_FORM);
  const [saving, setSaving] = useState(false);

  const update = (field: keyof typeof EMPTY_FORM) => (e: { target: { value: string } }) =>
    setForm((prev) => ({ ...prev, [field]: e.target.value }));

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!form.enterprisePk || !form.email) {
      showToast('Enterprise PK and Email are required', 'warning');
      return;
    }
    setSaving(true);
    const created = await onCreate({
      enterprise_pk: Number(form.enterprisePk),
      email: form.email,
      username: form.username,
    });
    setSaving(false);
    if (created) setForm(EMPTY_FORM);
  };

  return (
    <div className="card shadow-purple mb-3">
      <div className="card-header">
        <h6 className="mb-0">Create User</h6>
      </div>
      <div className="card-body">
        <form onSubmit={handleSubmit}>
          <div className="mb-2">
            <label className="form-label" htmlFor="create-enterprise-pk">
              Enterprise PK
            </label>
            <input
              id="create-enterprise-pk"
              className="form-control"
              type="number"
              value={form.enterprisePk}
              onChange={update('enterprisePk')}
              required
            />
          </div>
          <div className="mb-2">
            <label className="form-label" htmlFor="create-email">
              Email
            </label>
            <input
              id="create-email"
              className="form-control"
              type="email"
              value={form.email}
              onChange={update('email')}
              required
            />
          </div>
          <div className="mb-3">
            <label className="form-label" htmlFor="create-username">
              Username
            </label>
            <input
              id="create-username"
              className="form-control"
              type="text"
              value={form.username}
              onChange={update('username')}
            />
          </div>
          <button className="btn btn-primary" type="submit" disabled={saving}>
            {saving ? 'Saving...' : 'Create'}
          </button>
        </form>
      </div>
    </div>
  );
}
