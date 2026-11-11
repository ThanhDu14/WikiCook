import React from 'react';
import type { KitchenTimer, EpochMs } from '../domain/sessionModel';
import { calculateRemainingSeconds, formatTimerDisplay } from '../domain/timerEngine';
import { COOKING_VI } from '../i18n/vi';

export interface BackgroundTimerWidgetProps {
  visible: boolean;
  timers: KitchenTimer[];
  now?: EpochMs;
  onReturnToCooking: () => void;
  onDismiss: () => void;
}

export function BackgroundTimerWidget({
  visible,
  timers,
  now = Date.now(),
  onReturnToCooking,
  onDismiss,
}: BackgroundTimerWidgetProps): React.ReactElement | null {
  if (!visible || !timers || timers.length === 0) return null;

  const runningTimers = timers.filter((t) => t.status === 'RUNNING');
  const expiredTimers = timers.filter((t) => t.status === 'EXPIRED');

  // Find nearest expiring running timer
  let nearestTimer: KitchenTimer | null = null;
  let minRemaining = Infinity;

  for (const t of runningTimers) {
    const rem = calculateRemainingSeconds(t, now);
    if (rem < minRemaining) {
      minRemaining = rem;
      nearestTimer = t;
    }
  }

  const primaryTimer = nearestTimer || expiredTimers[0] || timers[0];
  const isExpired = primaryTimer.status === 'EXPIRED';
  const remainingSec = calculateRemainingSeconds(primaryTimer, now);

  return (
    <aside
      aria-label={COOKING_VI.floatingWidgetTitle}
      className="fixed bottom-6 right-6 z-40 max-w-sm rounded-2xl border border-orange-300 bg-white/95 p-4 shadow-2xl backdrop-blur-md transition-all dark:border-stone-700 dark:bg-stone-900/95 dark:text-white"
    >
      <div className="flex items-center justify-between gap-3 border-b border-stone-100 pb-2 dark:border-stone-800">
        <div className="flex items-center gap-1.5">
          <span className="text-base">⏳</span>
          <h4 className="text-sm font-bold text-stone-900 dark:text-white">
            <span>{COOKING_VI.floatingWidgetTitle}</span>{' '}
            <span>({timers.length})</span>
          </h4>
        </div>
        <button
          type="button"
          onClick={onDismiss}
          aria-label="Đóng tiện ích"
          className="rounded p-1 text-stone-400 hover:text-stone-600 dark:hover:text-stone-300"
        >
          ✕
        </button>
      </div>

      <div className="mt-3 flex items-center justify-between gap-4">
        <div>
          <p className="text-xs font-semibold text-stone-500 dark:text-stone-400 truncate max-w-[150px]">
            {primaryTimer.label}
          </p>
          <div className="flex items-center gap-2 mt-0.5">
            <span
              className={`text-xl font-mono font-extrabold ${
                isExpired ? 'text-red-600 animate-pulse' : 'text-orange-600 dark:text-orange-400'
              }`}
            >
              {formatTimerDisplay(remainingSec)}
            </span>
            {isExpired && (
              <span className="rounded bg-red-100 px-1.5 py-0.5 text-[10px] font-bold text-red-700 dark:bg-red-950 dark:text-red-300">
                Đã hoàn thành!
              </span>
            )}
          </div>
        </div>

        <button
          type="button"
          onClick={onReturnToCooking}
          className="rounded-xl bg-orange-600 px-3 py-2 text-xs font-bold text-white shadow hover:bg-orange-700 active:scale-95 transition"
        >
          {COOKING_VI.returnToCooking}
        </button>
      </div>
    </aside>
  );
}
