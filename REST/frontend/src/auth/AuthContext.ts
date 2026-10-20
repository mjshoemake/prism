import { createContext } from 'react';
import type { AuthResult } from '../api/types';

export interface AuthContextValue {
  /** The login response of the signed-in user, or null when signed out. */
  session: AuthResult | null;
  userId: string | null;
  /** The signed-in user's enterprise; every user belongs to exactly one. */
  enterprisePk: number | null;
  /** True while the user is signed in with a temporary password and must choose a new one. */
  mustChangePassword: boolean;
  signIn: (session: AuthResult) => void;
  signOut: () => void;
  /** Call after a successful password change to clear the temporary-password flag. */
  markPasswordChanged: () => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
