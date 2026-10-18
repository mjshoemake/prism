import type { User } from '../../api/types';
import { CreateUserForm } from './components/CreateUserForm';
import { UsersTable } from './components/UsersTable';
import { useUsers } from './useUsers';

export function UsersPage() {
  const { users, loading, error, reload, editUser, createUser, saveUser, deleteUser, setPassword } = useUsers();

  // TODO: replace the browser confirm/prompt dialogs with Bootstrap modals.
  const handleDelete = (user: User) => {
    if (window.confirm('Delete this user?')) void deleteUser(user);
  };

  const handleSetPassword = (user: User) => {
    const password = window.prompt('Enter a new password for this user:');
    if (password) void setPassword(user, password);
  };

  return (
    <div className="container py-3 w-100">
      <div className="row">
        <div className="col-lg-4">
          <CreateUserForm onCreate={createUser} />
        </div>
        <div className="col-lg-8">
          <UsersTable
            users={users}
            loading={loading}
            error={error}
            onRefresh={() => void reload()}
            onEdit={editUser}
            onSave={(user) => void saveUser(user)}
            onSetPassword={handleSetPassword}
            onDelete={handleDelete}
          />
        </div>
      </div>
    </div>
  );
}
