import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { strings } from '../strings.vi';
import type { ConfirmResult } from '../types';
import { shoppingImportPath } from '../lib/paths';

const SHOWN_MS = 10_000;

/**
 * After "Lưu thực đơn" skipped some dishes (US2-6): a snackbar at the bottom of the page (W-AI05, AI05) with what
 * was saved and how many were skipped, then on to the shopping list of the week. Goes away by itself.
 */
export function SavedNotice({ weekStart, result, onDismiss }: { weekStart: string; result: ConfirmResult; onDismiss: () => void }) {
  const navigate = useNavigate();
  useEffect(() => {
    const timer = setTimeout(onDismiss, SHOWN_MS);
    return () => clearTimeout(timer);
  }, [onDismiss]);
  return (
    <div className="fixed inset-x-0 bottom-ds-5 z-[60] flex justify-center px-ds-4">
      <div className="wk-snackbar" role="status">
        <span>{strings.saved(result.saved, result.skippedOccupied + result.skippedUnavailable)}</span>
        <button type="button" onClick={() => navigate(shoppingImportPath(weekStart))}>
          {strings.goShopping}
        </button>
      </div>
    </div>
  );
}
