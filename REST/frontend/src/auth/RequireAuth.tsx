import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuth } from './useAuth';

const CHANGE_PASSWORD_PATH = '/change-password';

/**
 * Route guard for signed-in pages:
 * - signed out → redirect to /login (then back to the requested page);
 * - signed in with a temporary password → every page redirects to /change-password until it is changed.
 *
 * This is a UI convenience only; the API must enforce the same rules (backlog #5).
 */
export function RequireAuth() {
  const { session, mustChangePassword } = useAuth();
  const location = useLocation();

  if (!session) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }
  if (mustChangePassword && location.pathname !== CHANGE_PASSWORD_PATH) {
    return <Navigate to={CHANGE_PASSWORD_PATH} replace />;
  }
  return <Outlet />;
}
