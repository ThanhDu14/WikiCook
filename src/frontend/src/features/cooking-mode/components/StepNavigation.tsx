import React, { useEffect } from 'react';
import { COOKING_VI } from '../i18n/vi';

export interface StepNavigationProps {
  currentStepIndex: number;
  totalSteps: number;
  onNext: () => void;
  onPrev: () => void;
  onSelectStep: (stepIndex: number) => void;
  onFinish?: () => void;
  onOpenIngredients?: () => void;
  hasActiveAlarm?: boolean;
  onSilenceAlarm?: () => void;
}

export function StepNavigation({
  currentStepIndex,
  totalSteps,
  onNext,
  onPrev,
  onSelectStep,
  onFinish,
  onOpenIngredients,
  hasActiveAlarm = false,
  onSilenceAlarm,
}: StepNavigationProps): React.ReactElement {
  const isFirstStep = currentStepIndex <= 0;
  const isLastStep = currentStepIndex >= totalSteps - 1;

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      const interactiveSelector =
        'button, a[href], area[href], input, textarea, select, summary, audio[controls], video[controls], [contenteditable]:not([contenteditable="false"]), [tabindex], [role]:not([role="presentation"]):not([role="none"])';
      const isInteractive = (target: EventTarget | null) =>
        target instanceof Element && Boolean(target.closest(interactiveSelector));

      if (
        e.defaultPrevented ||
        document.querySelector('dialog[open], [aria-modal="true"]') ||
        isInteractive(e.target) ||
        isInteractive(document.activeElement)
      ) return;

      // FR-008 & FR-014: Spacebar priority silences active alarm without advancing step
      if (e.key === ' ' && hasActiveAlarm && onSilenceAlarm) {
        e.preventDefault();
        onSilenceAlarm();
        return;
      }

      if (e.key === ' ' || e.key === 'ArrowRight') {
        e.preventDefault();
        if (!isLastStep) {
          onNext();
        }
      } else if (e.key === 'ArrowLeft') {
        e.preventDefault();
        if (!isFirstStep) {
          onPrev();
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
    };
  }, [currentStepIndex, isFirstStep, isLastStep, onNext, onPrev, onFinish, hasActiveAlarm, onSilenceAlarm]);

  return (
    <nav
      aria-label="Điều hướng bước nấu ăn"
      className="flex flex-wrap items-center justify-between gap-3 p-4 sm:p-6"
    >
      {/* Prev Step Button (>= 64x64px touch target) */}
      <button
        type="button"
        disabled={isFirstStep}
        onClick={onPrev}
        aria-label={COOKING_VI.prevStep}
        className="flex min-h-[64px] min-w-[64px] items-center justify-center gap-1.5 rounded-2xl bg-stone-200 px-6 py-4 text-lg font-bold text-stone-800 shadow-sm transition active:scale-95 disabled:cursor-not-allowed disabled:opacity-40 hover:bg-stone-300 dark:bg-stone-800 dark:text-stone-200 dark:hover:bg-stone-700"
      >
        <span>←</span>
        <span>{COOKING_VI.prevStep}</span>
      </button>

      {/* Middle Controls: Jump Picker & Ingredients Toggle */}
      <div className="flex items-center gap-2">
        <label className="flex items-center gap-1.5 text-sm font-semibold">
          <span className="sr-only">{COOKING_VI.jumpToStep}</span>
          <select
            aria-label={COOKING_VI.jumpToStep}
            value={currentStepIndex}
            onChange={(e) => onSelectStep(parseInt(e.target.value, 10))}
            className="min-h-[48px] rounded-xl border border-stone-300 bg-white px-3 py-2 text-base font-medium text-stone-800 shadow-sm dark:border-stone-700 dark:bg-stone-900 dark:text-white"
          >
            {Array.from({ length: totalSteps }, (_, idx) => (
              <option key={idx} value={idx}>
                {COOKING_VI.stepCounter(idx + 1, totalSteps)}
              </option>
            ))}
          </select>
        </label>

        {onOpenIngredients && (
          <button
            type="button"
            onClick={onOpenIngredients}
            className="flex min-h-[48px] items-center gap-1.5 rounded-xl border border-stone-300 bg-white px-4 py-2 text-sm font-semibold text-stone-700 shadow-sm hover:bg-stone-50 active:scale-95 transition dark:border-stone-700 dark:bg-stone-900 dark:text-stone-300"
          >
            🥕 {COOKING_VI.viewIngredients}
          </button>
        )}
      </div>

      {/* Next Step / Finish Button (>= 64x64px touch target) */}
      {isLastStep ? (
        <button
          type="button"
          onClick={onFinish}
          aria-label={COOKING_VI.finishCooking}
          className="flex min-h-[64px] min-w-[64px] items-center justify-center gap-1.5 rounded-2xl bg-emerald-600 px-6 py-4 text-lg font-bold text-white shadow-md transition active:scale-95 hover:bg-emerald-700 dark:bg-emerald-700 dark:hover:bg-emerald-600"
        >
          <span>✓</span>
          <span>{COOKING_VI.finishCooking}</span>
        </button>
      ) : (
        <button
          type="button"
          onClick={onNext}
          aria-label={COOKING_VI.nextStep}
          className="flex min-h-[64px] min-w-[64px] items-center justify-center gap-1.5 rounded-2xl bg-orange-600 px-6 py-4 text-lg font-bold text-white shadow-md transition active:scale-95 hover:bg-orange-700 dark:bg-orange-700 dark:hover:bg-orange-600"
        >
          <span>{COOKING_VI.nextStep}</span>
          <span>→</span>
        </button>
      )}
    </nav>
  );
}
