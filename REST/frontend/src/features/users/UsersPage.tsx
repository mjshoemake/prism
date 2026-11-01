import { useMemo, useState } from 'react';
import type { User } from '../../api/types';
import { useAuth } from '../../auth/useAuth';
import { ErrorAlert } from '../../components/ErrorAlert';
import { LoadingSpinner } from '../../components/LoadingSpinner';
import { AddUserModal } from './components/AddUserModal';
import { EditUserModal } from './components/EditUserModal';
import { UsersTable } from './components/UsersTable';
import { DEFAULT_SORT, visibleUsers, type SortKey, type SortState } from './userList';
import { useUsers } from './useUsers';

type OpenModal = { kind: 'add' } | { kind: 'edit'; userId: string } | null;

/**
 * User administration for the signed-in user's enterprise. Users are never deleted: they are
 * deactivated, and deactivated users are hidden unless "Show deactivated" is on.
 */
export function UsersPage() {
  const { enterprisePk, userId } = useAuth();
  const { users, loading, error, createUser, updateUser, resetPassword } = useUsers(enterprisePk);

  const [search, setSearch] = useState('');
  const [showInactive, setShowInactive] = useState(false);
  const [sort, setSort] = useState<SortState>(DEFAULT_SORT);
  const [modal, setModal] = useState<OpenModal>(null);

  const shown = useMemo(() => visibleUsers(users, showInactive, search, sort), [users, showInactive, search, sort]);
  const inactiveCount = users.filter((u) => !u.is_active).length;
  // Look the user up by id so the modal always shows the latest data after a reload.
  const editing: User | undefined = modal?.kind === 'edit' ? users.find((u) => u.user_id === modal.userId) : undefined;

  const toggleSort = (key: SortKey) =>
    setSort((s) => (s.key === key ? { key, direction: s.direction === 'asc' ? 'desc' : 'asc' } : { key, direction: 'asc' }));

  if (enterprisePk === null) {
    return (
      <div className="container py-3">
        <ErrorAlert message="Your session has no enterprise. Sign out and sign in again." />
      </div>
    );
  }

  const renderBody = () => {
    if (loading && users.length === 0) return <LoadingSpinner />;
    if (error) return <ErrorAlert message={error} />;
    if (shown.length === 0) {
      return (
        <p className="text-muted text-center py-4 mb-0">
          {users.length === 0 ? 'No users yet. Use "Add user" to create one.' : 'No users match.'}
        </p>
      );
    }
    return <UsersTable users={shown} currentUserId={userId} sort={sort} onSort={toggleSort} onSelect={(u) => setModal({ kind: 'edit', userId: u.user_id })} />;
  };

  return (
    <div className="container py-3 w-100">
      <div className="card shadow-purple page-card">
        <div className="card-header d-flex flex-wrap gap-2 align-items-center">
          <h5 className="mb-0 me-auto">
            Users <span className="badge text-bg-secondary align-middle">{shown.length}</span>
          </h5>
          <input
            type="search"
            className="form-control form-control-sm users-search"
            placeholder="Search name or email"
            aria-label="Search users"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
          <div className="form-check form-switch mb-0">
            <input
              id="show-inactive"
              className="form-check-input"
              type="checkbox"
              role="switch"
              checked={showInactive}
              onChange={(e) => setShowInactive(e.target.checked)}
            />
            <label className="form-check-label small" htmlFor="show-inactive">
              Show deactivated ({inactiveCount})
            </label>
          </div>
          <button type="button" className="btn btn-primary btn-sm" onClick={() => setModal({ kind: 'add' })}>
            <i className="fas fa-plus me-1" aria-hidden="true" />
            Add user
          </button>
        </div>
        <div className="card-body p-0">{renderBody()}</div>
      </div>

      {modal?.kind === 'add' && <AddUserModal onCreate={createUser} onClose={() => setModal(null)} />}
      {editing && (
        <EditUserModal
          key={editing.user_id}
          user={editing}
          isSelf={editing.user_id === userId}
          onSave={updateUser}
          onResetPassword={resetPassword}
          onClose={() => setModal(null)}
        />
      )}
    </div>
  );
}
