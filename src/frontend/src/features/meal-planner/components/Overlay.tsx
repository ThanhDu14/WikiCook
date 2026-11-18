import { useEffect, useId, useRef, type CSSProperties, type ReactNode } from 'react';

interface Props {
  title: string;
  onClose: () => void;
  children: (titleId: string) => ReactNode;
  /**
   * `sheet` = bottom sheet (MP-02), `page` = full panel (MP-03, MP-04), `dialog` = centered `wk-dialog`,
   * `modal` = desktop `wx-modal` (W-MP05)
   */
  variant: 'sheet' | 'page' | 'dialog' | 'modal';
  role?: 'dialog' | 'alertdialog';
  /** Extra style of the panel, e.g. the width of a `wx-modal`. */
  panelStyle?: CSSProperties;
}

/**
 * Modal layer on the `wk-scrim` of the screens (fixed to the viewport instead of the artboard). Esc and a click
 * on the scrim close it; focus moves inside on open and returns on close.
 */
export function Overlay({ title, onClose, children, variant, role = 'dialog', panelStyle }: Props) {
  const titleId = useId();
  const panel = useRef<HTMLDivElement>(null);
  const close = useRef(onClose);
  useEffect(() => {
    close.current = onClose;
  }, [onClose]);

  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    const first = panel.current?.querySelector<HTMLElement>('[data-autofocus], input, button');
    first?.focus();
    const focusableSelector =
      'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.stopPropagation();
        close.current();
        return;
      }
      if (event.key !== 'Tab' || !panel.current) return;
      const focusable = Array.from(panel.current.querySelectorAll<HTMLElement>(focusableSelector));
      if (focusable.length === 0) return;
      const head = focusable[0];
      const tail = focusable[focusable.length - 1];
      const active = document.activeElement;
      if (event.shiftKey && (active === head || active === panel.current)) {
        event.preventDefault();
        tail.focus();
      } else if (!event.shiftKey && active === tail) {
        event.preventDefault();
        head.focus();
      }
    };
    const node = panel.current;
    node?.addEventListener('keydown', onKey);
    return () => {
      node?.removeEventListener('keydown', onKey);
      previous?.focus?.();
    };
  }, []);

  const panelClass = {
    sheet: 'wk-sheet max-w-reading',
    dialog: 'wk-dialog',
    modal: 'wx-modal',
    page: 'flex h-full w-full max-w-reading flex-col bg-bg',
  }[variant];
  const scrimClass = { sheet: 'wk-scrim wk-scrim--sheet', modal: 'wx-overlay', dialog: 'wk-scrim', page: 'wk-scrim' }[variant];
  return (
    <div
      role="presentation"
      className={scrimClass}
      style={{ position: 'fixed', zIndex: 50, ...(variant === 'page' ? { padding: 0, placeItems: 'stretch center' } : {}) }}
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div ref={panel} role={role} aria-modal="true" aria-label={title} className={panelClass} style={variant === 'sheet' ? { justifySelf: 'center', ...panelStyle } : panelStyle}>
        {children(titleId)}
      </div>
    </div>
  );
}
