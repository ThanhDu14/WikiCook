import { useEffect } from 'react';
import { vi } from '../strings.vi';

/** Short notice at the bottom of the screen (`wk-snackbar`), e.g. "Thay đổi chưa được lưu" (FR-019). */
export function Snackbar({ message, onDismiss }: { message: string | null; onDismiss: () => void }) {
  useEffect(() => {
    if (!message) return;
    const timer = setTimeout(onDismiss, 6000);
    return () => clearTimeout(timer);
  }, [message, onDismiss]);
  return (
    <div role="status" aria-live="polite" className="fixed inset-x-0 bottom-ds-5 z-[60] flex justify-center px-ds-4">
      {message && (
        <div className="wk-snackbar">
          <span>{message}</span>
          <button type="button" onClick={onDismiss}>
            {vi.close}
          </button>
        </div>
      )}
    </div>
  );
}
