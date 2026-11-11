import React from 'react';
import type { KitchenTimer, EpochMs } from '../domain/sessionModel';
import { calculateRemainingSeconds, formatTimerDisplay } from '../domain/timerEngine';
import { COOKING_VI } from '../i18n/vi';

export interface TimerDrawerProps {
  timers: KitchenTimer[];
  now?: EpochMs;
  onPauseTimer: (timerId: string) => void;
  onResumeTimer: (timerId: string) => void;
  onExtendTimer: (timerId: string, seconds: number) => void;
  onAcknowledgeTimer: (timerId: string) => void;
  onDeleteTimer: (timerId: string) => void;
  onOpenCustomTimer: () => void;
}

export function TimerDrawer({
  timers,
  now = Date.now(),
  onPauseTimer,
  onResumeTimer,
  onExtendTimer,
  onAcknowledgeTimer,
  onDeleteTimer,
  onOpenCustomTimer,
}: TimerDrawerProps): React.ReactElement {
  return (
    <div className="w-full rounded-2xl border border-stone-200 bg-white/95 p-4 shadow-lg backdrop-blur-sm dark:border-stone-800 dark:bg-stone-900/95 dark:text-white">
      {/* Header with Add Custom Timer button */}
      <div className="flex items-center justify-between border-b border-stone-100 pb-3 dark:border-stone-800">
        <h3 className="text-base font-bold text-stone-900 dark:text-white">
          ⏳ Bộ đếm giờ ({timers.length})
        </h3>
        <button
          type="button"
          onClick={onOpenCustomTimer}
          className="rounded-xl bg-orange-50 px-3 py-1.5 text-xs sm:text-sm font-semibold text-orange-700 hover:bg-orange-100 active:scale-95 transition dark:bg-orange-950/40 dark:text-orange-300"
        >
          {COOKING_VI.addCustomTimer}
        </button>
      </div>

      {/* Timers List */}
      <div className="mt-3 space-y-3">
        {timers.length === 0 ? (
          <p className="py-2 text-center text-xs text-stone-400">Chưa có bộ đếm giờ nào.</p>
        ) : (
          timers.map((timer) => {
            const remaining = calculateRemainingSeconds(timer, now);
            const isRunning = timer.status === 'RUNNING';
            const isPaused = timer.status === 'PAUSED';
            const isUnacknowledgedExpired =
              timer.status === 'EXPIRED' && timer.acknowledgedAt === null;
            const isAcknowledgedExpired =
              timer.status === 'EXPIRED' && timer.acknowledgedAt !== null;

            const overdueMinutes =
              timer.status === 'EXPIRED'
                ? Math.max(1, Math.floor((now - timer.expiredAt) / 60000))
                : 0;

            return (
              <div
                key={timer.timerId}
                className={`flex flex-wrap items-center justify-between gap-3 rounded-xl border p-3.5 transition ${
                  isUnacknowledgedExpired
                    ? 'animate-pulse border-red-300 bg-red-50/80 dark:border-red-900 dark:bg-red-950/40'
                    : isAcknowledgedExpired
                    ? 'border-emerald-200 bg-emerald-50/50 dark:border-emerald-900/50 dark:bg-emerald-950/20'
                    : isPaused
                    ? 'border-stone-200 bg-stone-50 dark:border-stone-800 dark:bg-stone-800/40'
                    : 'border-orange-200 bg-orange-50/40 dark:border-orange-900/40 dark:bg-stone-800/80'
                }`}
              >
                {/* Timer Info & Countdown */}
                <div className="flex flex-col">
                  <span className="text-sm font-semibold text-stone-900 dark:text-white">
                    {timer.label}
                  </span>
                  <div className="flex items-center gap-2">
                    <span
                      className={`text-2xl font-mono font-bold tracking-tight ${
                        isUnacknowledgedExpired
                          ? 'text-red-600 dark:text-red-400'
                          : isAcknowledgedExpired
                          ? 'text-emerald-600 dark:text-emerald-400'
                          : isPaused
                          ? 'text-stone-500'
                          : 'text-orange-600 dark:text-orange-400'
                      }`}
                    >
                      {formatTimerDisplay(remaining)}
                    </span>

                    {isUnacknowledgedExpired && (
                      <span className="rounded-md bg-red-100 px-2 py-0.5 text-xs font-bold text-red-700 dark:bg-red-900 dark:text-red-200">
                        {COOKING_VI.overdueMinutes(overdueMinutes)}
                      </span>
                    )}

                    {isAcknowledgedExpired && (
                      <span className="rounded-md bg-emerald-100 px-2 py-0.5 text-xs font-bold text-emerald-700 dark:bg-emerald-900 dark:text-emerald-200">
                        ✓ {COOKING_VI.timerCompleted}
                      </span>
                    )}

                    {isPaused && (
                      <span className="rounded-md bg-stone-200 px-1.5 py-0.5 text-xs font-semibold text-stone-600 dark:bg-stone-700 dark:text-stone-300">
                        Đang tạm dừng
                      </span>
                    )}
                  </div>
                </div>

                {/* Actions */}
                <div className="flex items-center gap-1.5">
                  {/* Extension chips only for RUNNING per A04 */}
                  {isRunning && (
                    <>
                      <button
                        type="button"
                        onClick={() => onExtendTimer(timer.timerId, 60)}
                        className="rounded-lg border border-stone-300 bg-white px-2 py-1.5 text-xs font-semibold text-stone-700 hover:bg-stone-100 active:scale-95 transition dark:border-stone-700 dark:bg-stone-800 dark:text-stone-300"
                      >
                        {COOKING_VI.extend1Min}
                      </button>
                      <button
                        type="button"
                        onClick={() => onExtendTimer(timer.timerId, 300)}
                        className="rounded-lg border border-stone-300 bg-white px-2 py-1.5 text-xs font-semibold text-stone-700 hover:bg-stone-100 active:scale-95 transition dark:border-stone-700 dark:bg-stone-800 dark:text-stone-300"
                      >
                        {COOKING_VI.extend5Min}
                      </button>
                      <button
                        type="button"
                        onClick={() => onPauseTimer(timer.timerId)}
                        aria-label={COOKING_VI.pauseTimer}
                        className="rounded-lg bg-stone-200 p-2 text-stone-700 hover:bg-stone-300 active:scale-95 transition dark:bg-stone-700 dark:text-stone-200"
                      >
                        ⏸
                      </button>
                    </>
                  )}

                  {isPaused && (
                    <button
                      type="button"
                      onClick={() => onResumeTimer(timer.timerId)}
                      aria-label={COOKING_VI.resumeTimer}
                      className="rounded-lg bg-emerald-600 px-3 py-1.5 text-xs font-bold text-white hover:bg-emerald-700 active:scale-95 transition"
                    >
                      ▶ {COOKING_VI.resumeTimer}
                    </button>
                  )}

                  {isUnacknowledgedExpired && (
                    <button
                      type="button"
                      onClick={() => onAcknowledgeTimer(timer.timerId)}
                      className="flex items-center gap-1 rounded-lg bg-red-600 px-3.5 py-2 text-xs font-bold text-white shadow-md hover:bg-red-700 active:scale-95 transition"
                    >
                      <span>✓</span>
                      <span>{COOKING_VI.timerCompleted}</span>
                    </button>
                  )}

                  <button
                    type="button"
                    onClick={() => onDeleteTimer(timer.timerId)}
                    aria-label={COOKING_VI.deleteTimer}
                    className="rounded-lg p-2 text-stone-400 hover:bg-stone-100 hover:text-red-600 active:scale-95 transition dark:hover:bg-stone-800"
                  >
                    🗑
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
}
