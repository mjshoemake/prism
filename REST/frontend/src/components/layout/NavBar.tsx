import type { ReactNode } from 'react';
import { Link, useNavigate } from 'react-router';
import { useAuth } from '../../auth/useAuth';

export function NavBar() {
  const { session, signOut } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    signOut();
    navigate('/metrics');
  };

  return (
    <nav className="navbar navbar-expand-lg navbar-dark">
      <div className="container-fluid">
        <Link className="navbar-brand fs-4" to="/metrics">
          <i className="fas fa-chart-bar me-2" />
          Prism
        </Link>
        <button
          className="navbar-toggler"
          type="button"
          data-bs-toggle="collapse"
          data-bs-target="#navContent"
          aria-controls="navContent"
          aria-expanded="false"
          aria-label="Toggle navigation"
        >
          <span className="navbar-toggler-icon" />
        </button>

        <div className="collapse navbar-collapse" id="navContent">
          <ul className="navbar-nav me-auto mb-2 mb-lg-0">
            <NavDropdown title="Dashboard">
              <li>
                <Link className="dropdown-item" to="/metrics">
                  Metrics
                </Link>
              </li>
              <li>
                <Link className="dropdown-item" to="/dashboards">
                  Dashboards
                </Link>
              </li>
            </NavDropdown>
            <NavDropdown title="Admin">
              <li>
                <Link className="dropdown-item" to="/users">
                  Users
                </Link>
              </li>
              <li>
                <Link className="dropdown-item" to="/settings">
                  System Settings
                </Link>
              </li>
              <li>
                <Link className="dropdown-item" to="/change-password">
                  Change Password
                </Link>
              </li>
            </NavDropdown>
          </ul>

          <div className="d-flex">
            {session ? (
              <button type="button" className="btn btn-outline-secondary btn-sm" onClick={handleLogout}>
                Logout
              </button>
            ) : (
              <Link className="btn btn-outline-secondary btn-sm" to="/login">
                Login
              </Link>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}

function NavDropdown({ title, children }: { title: string; children: ReactNode }) {
  return (
    <li className="nav-item dropdown">
      <button
        type="button"
        className="nav-link dropdown-toggle"
        data-bs-toggle="dropdown"
        aria-expanded="false"
      >
        {title}
      </button>
      <ul className="dropdown-menu">{children}</ul>
    </li>
  );
}
