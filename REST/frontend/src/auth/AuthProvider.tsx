import { useCallback, useMemo, useState, type ReactNode } from 'react';
import type { AuthResult } from '../api/types';
import { AuthContext, type AuthContextValue } from './AuthContext';
import { clearSession, loadSession, saveSession } from './authStorage';

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthResult | null>(loadSession);

  const signIn = useCallback((next: AuthResult) => {
    saveSession(next);
    setSession(next);
  }, []);

  const signOut = useCallback(() => {
    clearSession();
    setSession(null);
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({ session, userId: session?.user?.user_id ?? null, signIn, signOut }),
    [session, signIn, signOut],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
