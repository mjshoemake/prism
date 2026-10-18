import type { User } from '../../../api/types';
import type { UserEdits } from '../useUsers';

interface UserRowProps {
  user: User;
  onEdit: (edits: UserEdits) => void;
  onSave: () => void;
  onSetPassword: () => void;
  onDelete: () => void;
}

export function UserRow({ user, onEdit, onSave, onSetPassword, onDelete }: UserRowProps) {
  return (
    <tr>
      <td>
        <input
          className="form-control form-control-sm"
          aria-label="Email"
          value={user.email ?? ''}
          onChange={(e) => onEdit({ email: e.target.value })}
        />
      </td>
      <td>
        <input
          className="form-control form-control-sm"
          aria-label="Username"
          value={user.username ?? ''}
          onChange={(e) => onEdit({ username: e.target.value })}
        />
      </td>
      <td>{user.enterprise_pk}</td>
      <td>
        <input
          type="checkbox"
          className="form-check-input"
          aria-label="Active"
          checked={!!user.is_active}
          onChange={(e) => onEdit({ is_active: e.target.checked })}
        />
      </td>
      <td>{user.is_temp_password ? 'Yes' : 'No'}</td>
      <td className="text-end">
        <div className="btn-group">
          <button type="button" className="btn btn-sm btn-outline-primary" onClick={onSave}>
            Save
          </button>
          <button type="button" className="btn btn-sm btn-outline-warning" onClick={onSetPassword}>
            Set Password
          </button>
          <button type="button" className="btn btn-sm btn-outline-secondary" onClick={onDelete}>
            Delete
          </button>
        </div>
      </td>
    </tr>
  );
}
