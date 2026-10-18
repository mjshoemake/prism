import { createContext } from 'react';
import type { AuthResult } from '../api/types';

export interface AuthContextValue {
  /** The login response of the signed-in user, or null when signed out. */
  session: AuthResult | null;
  userId: string | null;
  signIn: (session: AuthResult) => void;
  signOut: () => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
