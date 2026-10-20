import { useEffect, useId, useRef, type ReactNode } from 'react';
import { createPortal } from 'react-dom';

interface ModalProps {
  title: string;
  onClose: () => void;
  children: ReactNode;
  /** Buttons for the footer. Use `<button type="submit" form="…">` to submit a form in the body. */
  footer?: ReactNode;
  size?: 'sm' | 'lg';
}

const FOCUSABLE = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled])';

/**
 * Accessible Bootstrap-styled dialog, controlled by React: render it to open, unmount it to close.
 * Esc and the × button call `onClose`; focus moves into the dialog, stays there (Tab wraps),
 * and returns to the previously focused element when it closes. Clicking the backdrop does nothing,
 * so a half-filled form isn't lost by accident.
 */
export function Modal({ title, onClose, children, footer, size }: ModalProps) {
  const titleId = useId();
  const dialogRef = useRef<HTMLDivElement>(null);

  // Keep the latest onClose without re-running the focus effect.
  const onCloseRef = useRef(onClose);
  useEffect(() => {
    onCloseRef.current = onClose;
  }, [onClose]);

  useEffect(() => {
    const previouslyFocused = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    document.body.classList.add('modal-open');
    const dialog = dialogRef.current;
    const firstField = dialog?.querySelector<HTMLElement>('input:not([disabled]), select, textarea');
    (firstField ?? dialog?.querySelector<HTMLElement>(FOCUSABLE))?.focus();

    const onKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.preventDefault();
        onCloseRef.current();
        return;
      }
      if (e.key !== 'Tab' || !dialog) return;
      const items = Array.from(dialog.querySelectorAll<HTMLElement>(FOCUSABLE));
      if (items.length === 0) return;
      const first = items[0];
      const last = items[items.length - 1];
      if (e.shiftKey && document.activeElement === first) {
        e.preventDefault();
        last.focus();
      } else if (!e.shiftKey && document.activeElement === last) {
        e.preventDefault();
        first.focus();
      }
    };
    document.addEventListener('keydown', onKeyDown);

    return () => {
      document.removeEventListener('keydown', onKeyDown);
      document.body.classList.remove('modal-open');
      previouslyFocused?.focus();
    };
  }, []);

  return createPortal(
    <>
      <div className="modal-backdrop show" />
      <div className="modal d-block" role="dialog" aria-modal="true" aria-labelledby={titleId} tabIndex={-1}>
        <div className={`modal-dialog modal-dialog-centered modal-dialog-scrollable ${size ? `modal-${size}` : ''}`}>
          <div className="modal-content shadow-purple" ref={dialogRef}>
            <div className="modal-header">
              <h5 className="modal-title" id={titleId}>
                {title}
              </h5>
              <button type="button" className="btn-close btn-close-white" aria-label="Close" onClick={onClose} />
            </div>
            <div className="modal-body">{children}</div>
            {footer && <div className="modal-footer">{footer}</div>}
          </div>
        </div>
      </div>
    </>,
    document.body,
  );
}
