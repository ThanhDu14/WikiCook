import { createElement } from 'react';
import { registerAiPlannerDraft, registerAiPlannerEntry, type AiPlannerDraft } from '@/features/meal-planner/lib/aiPlannerEntry';
import { AiGenerateButton } from './components/AiGenerateButton';
import { DraftBar } from './components/DraftBar';
import { useAiDraft } from './hooks/useAiDraft';

/** The draft of a week for the meal planner page: its dishes on the calendar and the banner over it. */
function usePlannerDraft(weekStart: string): AiPlannerDraft {
  const { draft, draftItems } = useAiDraft(weekStart);
  if (!draft) return {};
  // changing the draft (move, replace, remove) comes with US2 (T053)
  return { draftItems, banner: createElement(DraftBar, { draft }) };
}

/**
 * Plugs spec 031 into the meal planner of 030 (T046, contracts/integration.md section 3): the "Tạo bằng AI" button
 * and the draft shown on `WeekGrid`/`DayView`. Called once at start-up, before the first render.
 */
export function registerAiPlanner(): void {
  registerAiPlannerEntry(AiGenerateButton);
  registerAiPlannerDraft(usePlannerDraft);
}
