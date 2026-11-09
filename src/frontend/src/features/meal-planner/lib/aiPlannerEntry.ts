import { createElement, type ComponentType, type ReactNode } from 'react';
import type { DraftChange, DraftItem } from '../types';

/** The "Tạo bằng AI" button of spec 031: it enables, disables or hides itself and opens its own dialog. */
export type AiGenerateButton = ComponentType<{ weekStart: string }>;

/**
 * The unsaved AI draft of a week (spec 031, T046): dishes for `WeekGrid`/`DayView`, where their changes go, and a
 * banner shown over the calendar while there is a draft.
 */
export interface AiPlannerDraft {
  draftItems?: DraftItem[];
  onDraftChange?: (change: DraftChange) => void;
  banner?: ReactNode;
}

/** A React hook of 031 that returns the draft of a week. */
export type AiPlannerDraftHook = (weekStart: string) => AiPlannerDraft;

let entry: AiGenerateButton | null = null;

const NO_DRAFT: AiPlannerDraft = {};
const noDraft: AiPlannerDraftHook = () => NO_DRAFT;
let draftHook: AiPlannerDraftHook = noDraft;

/**
 * Optional entry of the AI planner (contracts/integration.md section 3, T075). Spec 031 registers its button at
 * start-up; until then the planner shows no AI button and stays fully usable (FR-031). `null` unregisters it.
 */
export function registerAiPlannerEntry(button: AiGenerateButton | null): void {
  entry = button;
}

/** Spec 031 registers how the page reads the draft of a week, at start-up before the first render; `null` unregisters. */
export function registerAiPlannerDraft(hook: AiPlannerDraftHook | null): void {
  draftHook = hook ?? noDraft;
}

export function hasAiPlannerEntry(): boolean {
  return entry !== null;
}

/** The registered button for a week, or nothing. */
export function AiPlannerAction({ weekStart }: { weekStart: string }) {
  return entry ? createElement(entry, { weekStart }) : null;
}

/** The AI draft of `weekStart`, or none while 031 has not registered (the hook is fixed before the first render). */
export function useAiPlannerDraft(weekStart: string): AiPlannerDraft {
  return draftHook(weekStart);
}
