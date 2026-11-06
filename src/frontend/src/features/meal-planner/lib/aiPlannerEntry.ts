import { createElement, type ComponentType } from 'react';

/** The "Tạo bằng AI" button of spec 031: it enables, disables or hides itself and opens its own dialog. */
export type AiGenerateButton = ComponentType<{ weekStart: string }>;

let entry: AiGenerateButton | null = null;

/**
 * Optional entry of the AI planner (contracts/integration.md section 3, T075). Spec 031 registers its button at
 * start-up; until then the planner shows no AI button and stays fully usable (FR-031). `null` unregisters it.
 */
export function registerAiPlannerEntry(button: AiGenerateButton | null): void {
  entry = button;
}

export function hasAiPlannerEntry(): boolean {
  return entry !== null;
}

/** The registered button for a week, or nothing. */
export function AiPlannerAction({ weekStart }: { weekStart: string }) {
  return entry ? createElement(entry, { weekStart }) : null;
}
