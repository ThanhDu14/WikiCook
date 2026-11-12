import React from 'react';
import { COOKING_VI } from '../i18n/vi';

export interface ResumeSessionDialogProps {
  isOpen: boolean;
  stepNumber: number;
  onResume: () => void;
  onStartOver: () => void;
}

export function ResumeSessionDialog({
  isOpen,
  stepNumber,
  onResume,
  onStartOver,
}: ResumeSessionDialogProps): React.ReactElement | null {
  if (!isOpen) return null;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="resume-dialog-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm"
    >
      <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-2xl dark:bg-stone-900 dark:text-white">
        <h2 id="resume-dialog-title" className="text-xl font-bold text-stone-900 dark:text-white">
          {COOKING_VI.resumeModalTitle(stepNumber)}
        </h2>

        <p className="mt-3 text-sm text-stone-600 dark:text-stone-300">
          Bạn có muốn tiếp tục tiến trình nấu hiện tại hoặc bắt đầu lại từ bước đầu tiên?
        </p>

        <div className="mt-6 flex flex-col gap-3">
          <button
            type="button"
            onClick={onResume}
            className="w-full rounded-xl bg-orange-600 px-4 py-3.5 text-base font-semibold text-white shadow-md hover:bg-orange-700 active:scale-[0.98] transition"
          >
            {COOKING_VI.resumeButton}
          </button>

          <button
            type="button"
            onClick={onStartOver}
            className="w-full rounded-xl border border-stone-300 bg-stone-50 px-4 py-3 text-base font-semibold text-stone-700 hover:bg-stone-100 active:scale-[0.98] transition dark:border-stone-700 dark:bg-stone-800 dark:text-stone-200 dark:hover:bg-stone-700"
          >
            {COOKING_VI.startOverButton}
          </button>
        </div>
      </div>
    </div>
  );
}
