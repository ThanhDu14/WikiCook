import { useEffect, useId, useRef, type ReactNode } from 'react';

interface Props {
  title: string;
  onClose: () => void;
  children: (titleId: string) => ReactNode;
  /** `sheet` = bottom sheet (MP-02), `page` = full panel (MP-03, MP-04, MP-05), `dialog` = centered `wk-dialog` */
  variant: 'sheet' | 'page' | 'dialog';
  role?: 'dialog' | 'alertdialog';
}

/**
 * Modal layer on the `wk-scrim` of the screens (fixed to the viewport instead of the artboard). Esc and a click
 * on the scrim close it; focus moves inside on open and returns on close.
 */
export function Overlay({ title, onClose, children, variant, role = 'dialog' }: Props) {
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
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.stopPropagation();
        close.current();
      }
    };
    const node = panel.current;
    node?.addEventListener('keydown', onKey);
    return () => {
      node?.removeEventListener('keydown', onKey);
      previous?.focus?.();
    };
  }, []);

  const panelClass =
    variant === 'sheet' ? 'wk-sheet max-w-reading' : variant === 'dialog' ? 'wk-dialog' : 'flex h-full w-full max-w-reading flex-col bg-bg';
  return (
    <div
      role="presentation"
      className={variant === 'sheet' ? 'wk-scrim wk-scrim--sheet' : 'wk-scrim'}
      style={{ position: 'fixed', zIndex: 50, ...(variant === 'page' ? { padding: 0, placeItems: 'stretch center' } : {}) }}
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) onClose();
      }}
    >
      <div ref={panel} role={role} aria-modal="true" aria-label={title} className={panelClass} style={variant === 'sheet' ? { justifySelf: 'center' } : undefined}>
        {children(titleId)}
      </div>
    </div>
  );
}
