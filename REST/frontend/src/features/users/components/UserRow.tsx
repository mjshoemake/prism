import type { User } from '../../../api/types';
import { formatDateTime } from '../format';
import { displayName } from '../userList';

interface UserRowProps {
  user: User;
  isSelf: boolean;
  onSelect: () => void;
}

/** Read-only row; clicking it (or its Edit button) opens the Edit User modal. */
export function UserRow({ user, isSelf, onSelect }: UserRowProps) {
  return (
    <tr className={`user-row ${user.is_active ? '' : 'user-row--inactive'}`} onClick={onSelect}>
      <td>
        {displayName(user)}
        {isSelf && <span className="badge text-bg-secondary ms-2">You</span>}
      </td>
      <td>{user.email}</td>
      <td>
        {user.is_active ? (
          <span className="badge text-bg-success">Active</span>
        ) : (
          <span className="badge text-bg-secondary">Deactivated</span>
        )}
        {user.is_temp_password && <span className="badge text-bg-warning ms-1">Temp password</span>}
      </td>
      <td>{formatDateTime(user.last_login_at)}</td>
      <td>{formatDateTime(user.created_at)}</td>
      <td className="text-end">
        <button
          type="button"
          className="btn btn-sm btn-outline-secondary"
          aria-label={`Edit ${displayName(user)}`}
          onClick={(e) => {
            e.stopPropagation();
            onSelect();
          }}
        >
          <i className="fas fa-pen" aria-hidden="true" />
        </button>
      </td>
    </tr>
  );
}
