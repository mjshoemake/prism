import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuth } from './useAuth';

/** Route guard: renders the child route when signed in, otherwise redirects to /login. */
export function RequireAuth() {
  const { session } = useAuth();
  const location = useLocation();

  if (!session) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  return <Outlet />;
}
