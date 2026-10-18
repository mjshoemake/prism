import type { User } from '../../../api/types';
import { ErrorAlert } from '../../../components/ErrorAlert';
import { LoadingSpinner } from '../../../components/LoadingSpinner';
import type { UserEdits } from '../useUsers';
import { UserRow } from './UserRow';

interface UsersTableProps {
  users: User[];
  loading: boolean;
  error: string | null;
  onRefresh: () => void;
  onEdit: (userId: string, edits: UserEdits) => void;
  onSave: (user: User) => void;
  onSetPassword: (user: User) => void;
  onDelete: (user: User) => void;
}

export function UsersTable({ users, loading, error, onRefresh, onEdit, onSave, onSetPassword, onDelete }: UsersTableProps) {
  return (
    <div className="card shadow-purple">
      <div className="card-header d-flex justify-content-between align-items-center">
        <h6 className="mb-0">Users</h6>
        <button type="button" className="btn btn-sm btn-outline-secondary" onClick={onRefresh}>
          Refresh
        </button>
      </div>
      <div className="card-body">
        {loading ? (
          <LoadingSpinner />
        ) : error ? (
          <ErrorAlert message={error} />
        ) : (
          <div className="table-responsive">
            <table className="table table-dark table-hover align-middle">
              <thead>
                <tr>
                  <th>Email</th>
                  <th>Username</th>
                  <th>Enterprise</th>
                  <th>Active</th>
                  <th>Temp PW</th>
                  <th aria-label="Actions" />
                </tr>
              </thead>
              <tbody>
                {users.map((user) => (
                  <UserRow
                    key={user.user_id}
                    user={user}
                    onEdit={(edits) => onEdit(user.user_id, edits)}
                    onSave={() => onSave(user)}
                    onSetPassword={() => onSetPassword(user)}
                    onDelete={() => onDelete(user)}
                  />
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
