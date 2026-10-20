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

  const markPasswordChanged = useCallback(() => {
    setSession((current) => {
      if (!current?.user) return current;
      const next = { ...current, user: { ...current.user, is_temp_password: false } };
      saveSession(next);
      return next;
    });
  }, []);

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      userId: session?.user?.user_id ?? null,
      enterprisePk: session?.user?.enterprise_pk ?? null,
      mustChangePassword: session?.user?.is_temp_password === true,
      signIn,
      signOut,
      markPasswordChanged,
    }),
    [session, signIn, signOut, markPasswordChanged],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
