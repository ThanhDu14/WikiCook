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

/** wx-overlay + wx-modal/wx-drawer shell shared by the SL dialogs: Escape and the scrim close it, focus moves inside. */
export function Overlay({ title, subtitle, onClose, variant = 'modal', children }: OverlayProps) {
  const panel = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    // Keep an autoFocus field's focus; otherwise focus the panel so Escape and screen readers land inside.
    if (!panel.current?.contains(document.activeElement)) panel.current?.focus();
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
      previous?.focus?.();
    };
  }, [onClose]);

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
