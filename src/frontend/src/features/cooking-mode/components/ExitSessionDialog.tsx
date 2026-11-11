import React from 'react';
import { COOKING_VI } from '../i18n/vi';

export interface ExitSessionDialogProps {
  isOpen: boolean;
  runningCount: number;
  pausedCount: number;
  expiredCount: number;
  onKeepTimers: () => void;
  onDiscardAll: () => void;
  onCancel: () => void;
}

export function ExitSessionDialog({
  isOpen,
  runningCount,
  pausedCount,
  expiredCount,
  onKeepTimers,
  onDiscardAll,
  onCancel,
}: ExitSessionDialogProps): React.ReactElement | null {
  if (!isOpen) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="exit-dialog-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm"
    >
      <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl [.theme-high-contrast-dark_&]:bg-black [.theme-high-contrast-dark_&]:text-white">
        <h2 id="exit-dialog-title" className="text-xl font-bold text-stone-900 [.theme-high-contrast-dark_&]:text-white">
          {COOKING_VI.exitDialogTitle}
        </h2>

        <p className="mt-3 text-base text-stone-600 [.theme-high-contrast-dark_&]:text-stone-300">
          {COOKING_VI.exitDialogMessage(runningCount, pausedCount, expiredCount)}
        </p>

        <div className="mt-6 flex flex-col gap-3">
          <button
            type="button"
            onClick={onKeepTimers}
            className="w-full rounded-xl bg-orange-600 px-4 py-3.5 text-base font-semibold text-white shadow-md hover:bg-orange-700 active:scale-[0.98] transition"
          >
            {COOKING_VI.keepTimersButton}
          </button>

          <button
            type="button"
            onClick={onDiscardAll}
            className="w-full rounded-xl border border-red-300 bg-red-50 px-4 py-3 text-base font-semibold text-red-700 hover:bg-red-100 active:scale-[0.98] transition [.theme-high-contrast-dark_&]:border-red-900 [.theme-high-contrast-dark_&]:bg-red-950 [.theme-high-contrast-dark_&]:text-red-200 [.theme-high-contrast-dark_&]:hover:bg-red-900"
          >
            {COOKING_VI.discardAllButton}
          </button>

          <button
            type="button"
            onClick={onCancel}
            className="w-full rounded-xl border border-stone-300 bg-transparent px-4 py-2.5 text-sm font-medium text-stone-600 hover:bg-stone-100 active:scale-[0.98] transition [.theme-high-contrast-dark_&]:border-stone-700 [.theme-high-contrast-dark_&]:text-stone-400 [.theme-high-contrast-dark_&]:hover:bg-stone-800"
          >
            {COOKING_VI.cancel}
          </button>
        </div>
      </div>
    </div>
  );
}
