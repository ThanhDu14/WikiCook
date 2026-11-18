import { useEffect, useRef, type ReactNode } from 'react';
import { Icon } from './Icon';

interface ModalProps {
  /** Accessible name of the dialog (aria-label of the screens). */
  label: string;
  title: string;
  subtitle?: ReactNode;
  onClose: () => void;
  children: ReactNode;
}

const FOCUSABLE =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

/**
 * wx-overlay + wx-modal of the RV dialogs (same behaviour as the shopping-list Overlay): Escape and the scrim close
 * it, focus moves inside and stays there, and returns to the opener on close.
 */
export function Modal({ label, title, subtitle, onClose, children }: ModalProps) {
  const panel = useRef<HTMLDivElement>(null);
  const onCloseRef = useRef(onClose);
  useEffect(() => {
    onCloseRef.current = onClose;
  });

  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    if (!panel.current?.contains(document.activeElement)) panel.current?.focus();
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onCloseRef.current();
        return;
      }
      if (event.key !== 'Tab' || !panel.current) return;
      const focusable = Array.from(panel.current.querySelectorAll<HTMLElement>(FOCUSABLE));
      if (focusable.length === 0) return;
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      const active = document.activeElement;
      if (event.shiftKey && (active === first || active === panel.current)) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && active === last) {
        event.preventDefault();
        first.focus();
      }
    };
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
      previous?.focus?.();
    };
  }, []);

  return (
    <div
      role="presentation"
      className="wx-overlay"
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div ref={panel} className="wx-modal" role="dialog" aria-modal="true" aria-label={label} tabIndex={-1}>
        <div className="wx-modal__head">
          <div className="sx-stack" style={{ gap: 6 }}>
            <h2 className="wk-h2">{title}</h2>
            {subtitle && <p className="wk-muted">{subtitle}</p>}
          </div>
          <button className="wk-icon-btn wk-icon-btn--plain" type="button" aria-label="Đóng" onClick={onClose}>
            <Icon name="close" />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}
