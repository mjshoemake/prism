import { Outlet } from 'react-router';
import { NavBar } from './NavBar';

export function AppLayout() {
  return (
    <>
      <NavBar />
      <main className="app-main">
        <Outlet />
      </main>
    </>
  );
}
