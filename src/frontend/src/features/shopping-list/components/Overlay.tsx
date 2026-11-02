import { useEffect, useRef, type ReactNode } from 'react';
import { Icon } from './Icon';

interface OverlayProps {
  title: string;
  subtitle?: ReactNode;
  onClose: () => void;
  /** `modal` = centred wx-modal, `wide` = wx-modal--wide, `drawer` = right-hand wx-drawer (W-SL05). */
  variant?: 'modal' | 'wide' | 'drawer';
  children: ReactNode;
}

const FOCUSABLE = 'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

/**
 * wx-overlay + wx-modal/wx-drawer shell shared by the SL dialogs: Escape and the scrim close it,
 * focus moves inside, Tab stays inside (aria-modal) and returns to the opener on close.
 */
export function Overlay({ title, subtitle, onClose, variant = 'modal', children }: OverlayProps) {
  const panel = useRef<HTMLDivElement>(null);
  // Callers often pass an inline `onClose`; reading it through a ref keeps the effect below to one run per
  // opening, so a re-render no longer sends focus back to the opener and then into the panel again.
  const onCloseRef = useRef(onClose);
  useEffect(() => {
    onCloseRef.current = onClose;
  });

  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    // Keep an autoFocus field's focus; otherwise focus the panel so Escape and screen readers land inside.
    if (!panel.current?.contains(document.activeElement)) panel.current?.focus();
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onCloseRef.current();
        return;
      }
      if (event.key !== 'Tab' || !panel.current) return;
      const focusable = Array.from(panel.current.querySelectorAll<HTMLElement>(FOCUSABLE));
      if (focusable.length === 0) {
        event.preventDefault();
        return;
      }
      const first = focusable[0];
      const last = focusable[focusable.length - 1];
      const active = document.activeElement;
      if (event.shiftKey && (active === first || active === panel.current)) {
        event.preventDefault();
        last.focus();
      } else if (!event.shiftKey && active === last) {
        event.preventDefault();
        first.focus();
      } else if (!panel.current.contains(active)) {
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

  const panelClass = variant === 'drawer' ? 'wx-drawer' : variant === 'wide' ? 'wx-modal wx-modal--wide' : 'wx-modal';

  return (
    // The scrim click is a mouse shortcut only; keyboard users close with Escape or the close button.
    <div
      role="presentation"
      className={variant === 'drawer' ? 'wx-overlay wx-overlay--right' : 'wx-overlay'}
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div ref={panel} className={panelClass} role="dialog" aria-modal="true" aria-label={title} tabIndex={-1}>
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
