import { useState, type FormEvent } from 'react';
import { useLocation, useNavigate } from 'react-router';
import { errorMessage } from '../../api/client';
import { authApi } from '../../api/authApi';
import { useAuth } from '../../auth/useAuth';
import { useToast } from '../../components/toast/useToast';

export function LoginPage() {
  const { signIn } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!email || !password) {
      setMessage('Email and password are required');
      return;
    }

    setMessage(null);
    setLoading(true);
    try {
      const result = await authApi.login(email, password);
      if (!result.success) {
        setMessage(result.message || 'Login failed');
        setLoading(false);
        return;
      }
      signIn(result);
      showToast('Welcome!', 'success');
      const from = (location.state as { from?: string } | null)?.from;
      navigate(from && from !== '/login' ? from : '/metrics', { replace: true });
    } catch (err) {
      setMessage(`Login failed: ${errorMessage(err)}`);
      setLoading(false);
    }
  };

  return (
    <div className="container py-4 narrow-container">
      <div className="card shadow-purple">
        <div className="card-header">
          <h5 className="mb-0">Login</h5>
        </div>
        <div className="card-body">
          {message && (
            <div className="alert alert-warning" role="alert">
              {message}
            </div>
          )}
          <form onSubmit={handleSubmit}>
            <div className="mb-3">
              <label className="form-label" htmlFor="login-email">
                Email
              </label>
              <input
                id="login-email"
                className="form-control"
                type="email"
                autoComplete="username"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
              />
            </div>
            <div className="mb-3">
              <label className="form-label" htmlFor="login-password">
                Password
              </label>
              <input
                id="login-password"
                className="form-control"
                type="password"
                autoComplete="current-password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
              />
            </div>
            <button className="btn btn-primary" type="submit" disabled={loading}>
              {loading ? 'Logging in...' : 'Login'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
