import { useCallback, useMemo, useRef, useState, type ReactNode } from 'react';
import { ToastContext, type ToastVariant } from './ToastContext';

const TOAST_DURATION_MS = 3000;

interface Toast {
  message: string;
  variant: ToastVariant;
}

/** App-wide "snackbar": one message at a time, bottom-right, auto-dismissed. */
export function ToastProvider({ children }: { children: ReactNode }) {
  const [toast, setToast] = useState<Toast | null>(null);
  const timer = useRef<number | undefined>(undefined);

  const showToast = useCallback((message: string, variant: ToastVariant = 'info') => {
    setToast({ message, variant });
    window.clearTimeout(timer.current);
    timer.current = window.setTimeout(() => setToast(null), TOAST_DURATION_MS);
  }, []);

  const value = useMemo(() => ({ showToast }), [showToast]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      {toast && (
        <div className={`alert alert-${toast.variant} shadow-purple position-fixed app-toast`} role="status">
          {toast.message}
        </div>
      )}
    </ToastContext.Provider>
  );
}
