import { useRef, useState } from 'react';
import type { Timestamp } from '../../../api/types';
import { formatDateTime } from '../format';
import { copyToClipboard } from '../temporaryPassword';

interface TemporaryPasswordNoticeProps {
  /** Prefix for element ids, unique per page. */
  idPrefix: string;
  /** Generated temporary password, in clear text. */
  password: string;
  expiresAt: Timestamp | null | undefined;
  /** Who it's for, e.g. "jane@example.com". */
  recipient: string;
}

/**
 * Shows a newly generated temporary password once, with a Copy button, after Add user or Reset password.
 * The password is not stored anywhere in the browser and can't be shown again.
 */
export function TemporaryPasswordNotice({ idPrefix, password, expiresAt, recipient }: TemporaryPasswordNoticeProps) {
  const inputRef = useRef<HTMLInputElement>(null);
  const [copied, setCopied] = useState<boolean | null>(null);

  const handleCopy = async () => setCopied(await copyToClipboard(password, inputRef.current));

  return (
    <div className="border rounded p-3 temp-password-notice" role="status">
      <label className="form-label" htmlFor={`${idPrefix}-value`}>
        Temporary password for {recipient}
      </label>
      <div className="input-group">
        <input
          ref={inputRef}
          id={`${idPrefix}-value`}
          className="form-control font-monospace temp-password-value"
          type="text"
          readOnly
          value={password}
          onFocus={() => inputRef.current?.select()}
        />
        <button type="button" className="btn btn-primary" onClick={handleCopy}>
          <i className={`fas ${copied ? 'fa-check' : 'fa-copy'} me-1`} aria-hidden="true" />
          {copied ? 'Copied' : 'Copy'}
        </button>
      </div>
      {copied === false && (
        <small className="form-text text-warning d-block">Copy isn't available here — select the text and copy it.</small>
      )}
      <small className="form-text text-muted d-block mt-2">
        Valid until {formatDateTime(expiresAt)}. The user must choose a new password when they first sign in.{' '}
        <strong>This password won't be shown again.</strong>
      </small>
    </div>
  );
}
