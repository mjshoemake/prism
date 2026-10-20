import { MIN_PASSWORD_LENGTH, type TemporaryPasswordChoice } from '../temporaryPassword';

interface TemporaryPasswordFieldsProps {
  /** Prefix for element ids, unique per form. */
  idPrefix: string;
  value: TemporaryPasswordChoice;
  onChange: (value: TemporaryPasswordChoice) => void;
  disabled?: boolean;
}

/** "Enterprise default" vs "Set a temporary password" choice, used by Add User and Reset Password. */
export function TemporaryPasswordFields({ idPrefix, value, onChange, disabled = false }: TemporaryPasswordFieldsProps) {
  const set = (changes: Partial<TemporaryPasswordChoice>) => onChange({ ...value, ...changes });

  return (
    <fieldset disabled={disabled}>
      <legend className="form-label fs-6">Temporary password</legend>
      <div className="form-check">
        <input
          id={`${idPrefix}-default`}
          className="form-check-input"
          type="radio"
          name={`${idPrefix}-mode`}
          checked={value.mode === 'default'}
          onChange={() => set({ mode: 'default' })}
        />
        <label className="form-check-label" htmlFor={`${idPrefix}-default`}>
          Use the enterprise default temporary password
        </label>
      </div>
      <div className="form-check mb-2">
        <input
          id={`${idPrefix}-custom`}
          className="form-check-input"
          type="radio"
          name={`${idPrefix}-mode`}
          checked={value.mode === 'custom'}
          onChange={() => set({ mode: 'custom' })}
        />
        <label className="form-check-label" htmlFor={`${idPrefix}-custom`}>
          Set a temporary password
        </label>
      </div>

      {value.mode === 'custom' && (
        <div className="row g-2">
          <div className="col-sm-6">
            <label className="form-label" htmlFor={`${idPrefix}-password`}>
              Password
            </label>
            <input
              id={`${idPrefix}-password`}
              className="form-control"
              type="password"
              autoComplete="new-password"
              value={value.password}
              onChange={(e) => set({ password: e.target.value })}
            />
          </div>
          <div className="col-sm-6">
            <label className="form-label" htmlFor={`${idPrefix}-confirm`}>
              Confirm
            </label>
            <input
              id={`${idPrefix}-confirm`}
              className="form-control"
              type="password"
              autoComplete="new-password"
              value={value.confirm}
              onChange={(e) => set({ confirm: e.target.value })}
            />
          </div>
          <div className="col-12">
            <small className="form-text text-muted">At least {MIN_PASSWORD_LENGTH} characters.</small>
          </div>
        </div>
      )}
      <small className="form-text text-muted d-block mt-1">
        The user must choose a new password the next time they sign in.
      </small>
    </fieldset>
  );
}
