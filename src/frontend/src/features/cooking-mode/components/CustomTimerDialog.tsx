import React, { useState } from 'react';
import { COOKING_VI } from '../i18n/vi';

export interface CustomTimerDialogProps {
  isOpen: boolean;
  initialValue?: { label: string; durationSeconds: number } | null;
  notice?: string | null;
  onClose: () => void;
  onStartTimer: (data: { label: string; durationSeconds: number }) => void;
}

export function CustomTimerDialog({
  isOpen,
  initialValue,
  notice,
  onClose,
  onStartTimer,
}: CustomTimerDialogProps): React.ReactElement | null {
  const duration = initialValue?.durationSeconds ?? 0;
  const [label, setLabel] = useState(initialValue?.label ?? '');
  const [hours, setHours] = useState(String(Math.floor(duration / 3600)));
  const [minutes, setMinutes] = useState(String(Math.floor(duration % 3600 / 60)));
  const [seconds, setSeconds] = useState(String(duration % 60));

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const h = parseInt(hours, 10) || 0;
    const m = parseInt(minutes, 10) || 0;
    const s = parseInt(seconds, 10) || 0;

    const totalSeconds = h * 3600 + m * 60 + s;
    if (totalSeconds <= 0 || totalSeconds > 86400) return;

    onStartTimer({
      label: label.trim() || 'Hẹn giờ tùy chỉnh',
      durationSeconds: totalSeconds,
    });
    onClose();
  };

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="custom-timer-title"
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm"
    >
      <div className="w-full max-w-sm rounded-2xl bg-white p-6 shadow-2xl [.theme-high-contrast-dark_&]:bg-black [.theme-high-contrast-dark_&]:text-white">
        <h2 id="custom-timer-title" className="text-xl font-bold text-stone-900 [.theme-high-contrast-dark_&]:text-white">
          {COOKING_VI.customTimerTitle}
        </h2>

        {notice && <p className="mt-2 text-sm">{notice}</p>}
        <form onSubmit={handleSubmit} className="mt-4 space-y-4">
          <div>
            <label htmlFor="custom-timer-label" className="block text-xs font-semibold uppercase tracking-wider text-stone-500 [.theme-high-contrast-dark_&]:text-stone-400">
              Nhãn bộ đếm
            </label>
            <input
              id="custom-timer-label"
              type="text"
              value={label}
              onChange={(e) => setLabel(e.target.value)}
              placeholder={COOKING_VI.timerLabelPlaceholder}
              className="mt-1 w-full rounded-xl border border-stone-300 bg-white px-3.5 py-2.5 text-sm text-stone-900 shadow-sm focus:border-orange-500 focus:outline-none [.theme-high-contrast-dark_&]:border-stone-700 [.theme-high-contrast-dark_&]:bg-stone-800 [.theme-high-contrast-dark_&]:text-white"
            />
          </div>

          <div className="grid grid-cols-3 gap-2">
            <div>
              <label className="block text-xs font-semibold text-stone-500 [.theme-high-contrast-dark_&]:text-stone-400">
                {COOKING_VI.timerHours}
              </label>
              <input
                type="number"
                min="0"
                max="23"
                value={hours}
                aria-label={COOKING_VI.timerHours}
                onChange={(e) => setHours(e.target.value)}
                className="mt-1 w-full rounded-xl border border-stone-300 bg-white px-3 py-2 text-center text-lg font-bold text-stone-900 shadow-sm focus:border-orange-500 focus:outline-none [.theme-high-contrast-dark_&]:border-stone-700 [.theme-high-contrast-dark_&]:bg-stone-800 [.theme-high-contrast-dark_&]:text-white"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-stone-500 [.theme-high-contrast-dark_&]:text-stone-400">
                {COOKING_VI.timerMinutes}
              </label>
              <input
                type="number"
                min="0"
                max="59"
                value={minutes}
                aria-label={COOKING_VI.timerMinutes}
                onChange={(e) => setMinutes(e.target.value)}
                className="mt-1 w-full rounded-xl border border-stone-300 bg-white px-3 py-2 text-center text-lg font-bold text-stone-900 shadow-sm focus:border-orange-500 focus:outline-none [.theme-high-contrast-dark_&]:border-stone-700 [.theme-high-contrast-dark_&]:bg-stone-800 [.theme-high-contrast-dark_&]:text-white"
              />
            </div>

            <div>
              <label className="block text-xs font-semibold text-stone-500 [.theme-high-contrast-dark_&]:text-stone-400">
                {COOKING_VI.timerSeconds}
              </label>
              <input
                type="number"
                min="0"
                max="59"
                value={seconds}
                aria-label={COOKING_VI.timerSeconds}
                onChange={(e) => setSeconds(e.target.value)}
                className="mt-1 w-full rounded-xl border border-stone-300 bg-white px-3 py-2 text-center text-lg font-bold text-stone-900 shadow-sm focus:border-orange-500 focus:outline-none [.theme-high-contrast-dark_&]:border-stone-700 [.theme-high-contrast-dark_&]:bg-stone-800 [.theme-high-contrast-dark_&]:text-white"
              />
            </div>
          </div>

          <div className="mt-6 flex flex-col gap-2 pt-2">
            <button
              type="submit"
              className="w-full rounded-xl bg-orange-600 px-4 py-3 text-base font-semibold text-white shadow-md hover:bg-orange-700 active:scale-95 transition"
            >
              {COOKING_VI.startTimer}
            </button>
            <button
              type="button"
              onClick={onClose}
              className="w-full rounded-xl border border-stone-300 bg-transparent px-4 py-2.5 text-sm font-medium text-stone-600 hover:bg-stone-100 active:scale-95 transition [.theme-high-contrast-dark_&]:border-stone-700 [.theme-high-contrast-dark_&]:text-stone-400 [.theme-high-contrast-dark_&]:hover:bg-stone-800"
            >
              {COOKING_VI.cancel}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
