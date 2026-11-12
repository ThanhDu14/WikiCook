import React from 'react';
import type { InstructionStep } from '../domain/sessionModel';
import { resolveStepDurations } from '../domain/durationParser';
import { COOKING_VI } from '../i18n/vi';

export interface StepQuickTimersProps {
  step: InstructionStep;
  onStartQuickTimer: (seconds: number, label: string) => void;
}

function formatDurationChip(seconds: number): string {
  const hrs = Math.floor(seconds / 3600);
  const mins = Math.floor((seconds % 3600) / 60);
  const secs = seconds % 60;

  if (hrs > 0 && mins > 0) return `${hrs} giờ ${mins} phút`;
  if (hrs > 0) return `${hrs} giờ`;
  if (mins > 0 && secs > 0) return `${mins} phút ${secs} giây`;
  if (mins > 0) return `${mins} phút`;
  return `${secs} giây`;
}

export function StepQuickTimers({
  step,
  onStartQuickTimer,
}: StepQuickTimersProps): React.ReactElement | null {
  const resolved = resolveStepDurations(step.timerSeconds, step.instructionText);
  const allCandidates = [
    ...(resolved.primary ? [resolved.primary] : []),
    ...resolved.alternatives,
  ];

  if (allCandidates.length === 0) return null;

  return (
    <div className="flex flex-wrap items-center justify-center gap-2.5 my-3">
      {allCandidates.map((cand, idx) => {
        const durationText = formatDurationChip(cand.seconds);
        const lowerText = cand.lowerSeconds ? formatDurationChip(cand.lowerSeconds) : null;

        return (
          <React.Fragment key={idx}>
            <button
              type="button"
              onClick={() => onStartQuickTimer(cand.seconds, cand.label)}
              className="flex items-center gap-1.5 rounded-full border border-orange-200 bg-orange-50 px-4 py-2 text-sm font-semibold text-orange-800 shadow-sm transition hover:bg-orange-100 active:scale-95 [.theme-high-contrast-dark_&]:border-yellow-400 [.theme-high-contrast-dark_&]:bg-black [.theme-high-contrast-dark_&]:text-yellow-200 [.theme-high-contrast-dark_&]:hover:bg-yellow-900"
            >
              <span>⏱️</span>
              <span>{durationText}</span>
              {cand.source === 'AUTHOR' ? (
                <span className="text-xs opacity-75">({COOKING_VI.authorTimerLabel})</span>
              ) : (
                <span className="text-xs opacity-75">({cand.label})</span>
              )}
            </button>

            {cand.lowerSeconds && lowerText && (
              <button
                type="button"
                onClick={() => onStartQuickTimer(cand.lowerSeconds!, cand.label)}
                className="flex items-center gap-1.5 rounded-full border border-stone-200 bg-stone-100 px-3.5 py-1.5 text-xs font-semibold text-stone-700 shadow-sm transition hover:bg-stone-200 active:scale-95 [.theme-high-contrast-dark_&]:border-yellow-400 [.theme-high-contrast-dark_&]:bg-black [.theme-high-contrast-dark_&]:text-yellow-200 [.theme-high-contrast-dark_&]:hover:bg-yellow-900"
              >
                <span>⏱️</span>
                <span>{lowerText}</span>
                <span className="text-[11px] opacity-75">(tối thiểu)</span>
              </button>
            )}
          </React.Fragment>
        );
      })}
    </div>
  );
}
