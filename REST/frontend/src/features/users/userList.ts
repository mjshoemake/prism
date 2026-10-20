import type { User } from '../../api/types';
import { timestampValue } from './format';

export type SortKey = 'name' | 'email' | 'status' | 'lastLogin' | 'created';

export interface SortState {
  key: SortKey;
  direction: 'asc' | 'desc';
}

export const DEFAULT_SORT: SortState = { key: 'name', direction: 'asc' };

/** Display name: username, falling back to email. */
export function displayName(user: User): string {
  return user.username?.trim() || user.email;
}

const sortValue: Record<SortKey, (u: User) => string | number> = {
  name: (u) => displayName(u).toLowerCase(),
  email: (u) => u.email.toLowerCase(),
  status: (u) => (u.is_active ? 0 : 1),
  lastLogin: (u) => timestampValue(u.last_login_at),
  created: (u) => timestampValue(u.created_at),
};

/** Applies the "show deactivated" switch, the search text and the sort. */
export function visibleUsers(users: readonly User[], showInactive: boolean, search: string, sort: SortState): User[] {
  const needle = search.trim().toLowerCase();
  const value = sortValue[sort.key];
  const factor = sort.direction === 'asc' ? 1 : -1;
  return users
    .filter((u) => showInactive || u.is_active)
    .filter((u) => !needle || u.email.toLowerCase().includes(needle) || (u.username ?? '').toLowerCase().includes(needle))
    .sort((a, b) => {
      const x = value(a);
      const y = value(b);
      if (x < y) return -factor;
      if (x > y) return factor;
      return displayName(a).localeCompare(displayName(b));
    });
}
