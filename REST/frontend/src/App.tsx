import { HashRouter, Navigate, Route, Routes } from 'react-router';
import { AuthProvider } from './auth/AuthProvider';
import { RequireAuth } from './auth/RequireAuth';
import { AppLayout } from './components/layout/AppLayout';
import { ToastProvider } from './components/toast/ToastProvider';
import { ChangePasswordPage } from './features/auth/ChangePasswordPage';
import { LoginPage } from './features/auth/LoginPage';
import { MetricsPage } from './features/metrics/MetricsPage';
import { UsersPage } from './features/users/UsersPage';

/**
 * Routes use the URL hash (e.g. /#/users) so Spring Boot doesn't need a fallback
 * route to index.html for client-side paths.
 */
export default function App() {
  return (
    <AuthProvider>
      <ToastProvider>
        <HashRouter>
          <Routes>
            <Route element={<AppLayout />}>
              <Route path="/login" element={<LoginPage />} />
              <Route element={<RequireAuth />}>
                <Route path="/metrics" element={<MetricsPage />} />
                <Route path="/users" element={<UsersPage />} />
                <Route path="/change-password" element={<ChangePasswordPage />} />
              </Route>
              <Route path="*" element={<Navigate to="/metrics" replace />} />
            </Route>
          </Routes>
        </HashRouter>
      </ToastProvider>
    </AuthProvider>
  );
}
