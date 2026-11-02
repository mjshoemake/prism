import { HashRouter, Navigate, Route, Routes } from 'react-router';
import { AuthProvider } from './auth/AuthProvider';
import { RequireAuth } from './auth/RequireAuth';
import { AppLayout } from './components/layout/AppLayout';
import { ToastProvider } from './components/toast/ToastProvider';
import { AnalysisPage } from './features/analysis/AnalysisPage';
import { ChangePasswordPage } from './features/auth/ChangePasswordPage';
import { LoginPage } from './features/auth/LoginPage';
import { DashboardEditPage } from './features/dashboards/DashboardEditPage';
import { DashboardListPage } from './features/dashboards/DashboardListPage';
import { DataSourceEditPage } from './features/dataSources/DataSourceEditPage';
import { DataSourceListPage } from './features/dataSources/DataSourceListPage';
import { SystemSettingsPage } from './features/settings/SystemSettingsPage';
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
                <Route path="/analysis" element={<AnalysisPage />} />
                <Route path="/dashboards" element={<DashboardListPage />} />
                <Route path="/dashboards/new" element={<DashboardEditPage key="new" />} />
                <Route path="/dashboards/:dashboardPk/edit" element={<DashboardEditPage />} />
                <Route path="/data-sources" element={<DataSourceListPage />} />
                <Route path="/data-sources/new" element={<DataSourceEditPage key="new" />} />
                <Route path="/data-sources/:dataSourcePk/edit" element={<DataSourceEditPage />} />
                <Route path="/users" element={<UsersPage />} />
                <Route path="/settings" element={<SystemSettingsPage />} />
                <Route path="/change-password" element={<ChangePasswordPage />} />
              </Route>
              {/* The Metrics page became Analysis (2026-10-31); old links still work. */}
              <Route path="/metrics" element={<Navigate to="/analysis" replace />} />
              <Route path="*" element={<Navigate to="/analysis" replace />} />
            </Route>
          </Routes>
        </HashRouter>
      </ToastProvider>
    </AuthProvider>
  );
}
