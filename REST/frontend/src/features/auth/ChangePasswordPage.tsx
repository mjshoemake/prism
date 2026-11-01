import { useState, type FormEvent } from 'react';
import { useNavigate } from 'react-router';
import { errorMessage } from '../../api/client';
import { authApi } from '../../api/authApi';
import { useAuth } from '../../auth/useAuth';
import { useToast } from '../../components/toast/useToast';

const MIN_PASSWORD_LENGTH = 8;

function validate(current: string, next: string, confirm: string): string | null {
  if (!current || !next || !confirm) return 'All fields are required';
  if (next.length < MIN_PASSWORD_LENGTH) return `New password must be at least ${MIN_PASSWORD_LENGTH} characters long`;
  if (next !== confirm) return 'New password and confirmation do not match';
  return null;
}

export function ChangePasswordPage() {
  const { userId, mustChangePassword, markPasswordChanged } = useAuth();
  const { showToast } = useToast();
  const navigate = useNavigate();

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setMessage(null);

    const validationError = validate(currentPassword, newPassword, confirmPassword);
    if (validationError) {
      setMessage(validationError);
      return;
    }
    if (!userId) {
      setMessage('User information not available. Please log in again.');
      return;
    }

    setLoading(true);
    try {
      const result = await authApi.changePassword(userId, currentPassword, newPassword);
      if (!result.success) {
        setMessage(result.message || 'Password change failed');
        setLoading(false);
        return;
      }
      markPasswordChanged();
      showToast('Password changed successfully', 'success');
      navigate('/metrics');
    } catch (err) {
      setMessage(`Password change failed: ${errorMessage(err)}`);
      setLoading(false);
    }
  };

  return (
    <div className="container py-4 narrow-container">
      <div className="card shadow-purple page-card">
        <div className="card-header">
          <h5 className="mb-0">Change Password</h5>
        </div>
        <div className="card-body">
          {mustChangePassword && (
            <div className="alert alert-info" role="status">
              You signed in with a temporary password. Choose a new password to continue.
            </div>
          )}
          {message && (
            <div className="alert alert-warning" role="alert">
              {message}
            </div>
          )}
          <form onSubmit={handleSubmit}>
            <PasswordField
              id="current-password"
              label={mustChangePassword ? 'Temporary Password' : 'Current Password'}
              autoComplete="current-password"
              value={currentPassword}
              onChange={setCurrentPassword}
              disabled={loading}
            />
            <PasswordField
              id="new-password"
              label="New Password"
              autoComplete="new-password"
              value={newPassword}
              onChange={setNewPassword}
              disabled={loading}
              hint={`Must be at least ${MIN_PASSWORD_LENGTH} characters long`}
            />
            <PasswordField
              id="confirm-password"
              label="Confirm New Password"
              autoComplete="new-password"
              value={confirmPassword}
              onChange={setConfirmPassword}
              disabled={loading}
            />
            <button className="btn btn-primary" type="submit" disabled={loading}>
              {loading ? 'Changing Password...' : 'Change Password'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

interface PasswordFieldProps {
  id: string;
  label: string;
  autoComplete: string;
  value: string;
  onChange: (value: string) => void;
  disabled: boolean;
  hint?: string;
}

function PasswordField({ id, label, autoComplete, value, onChange, disabled, hint }: PasswordFieldProps) {
  return (
    <div className="mb-3">
      <label className="form-label" htmlFor={id}>
        {label}
      </label>
      <input
        id={id}
        className="form-control"
        type="password"
        autoComplete={autoComplete}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={disabled}
      />
      {hint && <small className="form-text text-muted">{hint}</small>}
    </div>
  );
}
