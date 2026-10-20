import type { User } from '../../../api/types';
import type { SortKey, SortState } from '../userList';
import { UserRow } from './UserRow';

interface UsersTableProps {
  users: User[];
  currentUserId: string | null;
  sort: SortState;
  onSort: (key: SortKey) => void;
  onSelect: (user: User) => void;
}

const COLUMNS: { key: SortKey; label: string }[] = [
  { key: 'name', label: 'Name' },
  { key: 'email', label: 'Email' },
  { key: 'status', label: 'Status' },
  { key: 'lastLogin', label: 'Last sign-in' },
  { key: 'created', label: 'Created' },
];

export function UsersTable({ users, currentUserId, sort, onSort, onSelect }: UsersTableProps) {
  return (
    <div className="table-responsive">
      <table className="table table-dark table-hover align-middle mb-0 users-table">
        <thead>
          <tr>
            {COLUMNS.map(({ key, label }) => {
              const active = sort.key === key;
              return (
                <th
                  key={key}
                  scope="col"
                  aria-sort={active ? (sort.direction === 'asc' ? 'ascending' : 'descending') : 'none'}
                >
                  <button type="button" className="btn btn-link sort-header p-0" onClick={() => onSort(key)}>
                    {label}
                    <i
                      className={`fas ms-1 ${active ? (sort.direction === 'asc' ? 'fa-sort-up' : 'fa-sort-down') : 'fa-sort text-muted'}`}
                      aria-hidden="true"
                    />
                  </button>
                </th>
              );
            })}
            <th scope="col">
              <span className="visually-hidden">Actions</span>
            </th>
          </tr>
        </thead>
        <tbody>
          {users.map((user) => (
            <UserRow
              key={user.user_id}
              user={user}
              isSelf={user.user_id === currentUserId}
              onSelect={() => onSelect(user)}
            />
          ))}
        </tbody>
      </table>
    </div>
  );
}
