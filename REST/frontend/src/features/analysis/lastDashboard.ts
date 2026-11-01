/**
 * The dashboard last opened on the Analysis page, remembered in this browser so the page reopens on it.
 * A convenience only: storage can be unavailable (private mode, blocked site data), and then nothing is remembered.
 */
const STORAGE_KEY = 'prism_last_dashboard';

export function loadLastDashboard(userId: string): number | null {
  try {
    const stored = JSON.parse(localStorage.getItem(STORAGE_KEY) ?? 'null') as { userId?: string; dashboardPk?: number } | null;
    return stored?.userId === userId && typeof stored.dashboardPk === 'number' ? stored.dashboardPk : null;
  } catch {
    return null;
  }
}

export function saveLastDashboard(userId: string, dashboardPk: number): void {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ userId, dashboardPk }));
  } catch {
    // Storage unavailable: the page just opens on the first dashboard next time.
  }
}
