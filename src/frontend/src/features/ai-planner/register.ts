import { createElement, useCallback, useState } from 'react';
import {
  registerAiPlannerDraft,
  registerAiPlannerEntry,
  type AiPlannerDraft,
  type AiPlannerPageContext,
} from '@/features/meal-planner/lib/aiPlannerEntry';
import type { DraftChange } from '@/features/meal-planner/types';
import { AiGenerateButton } from './components/AiGenerateButton';
import { DraftLayer } from './components/DraftLayer';
import { LeaveDraftDialog } from './components/LeaveDraftDialog';
import { SavedNotice } from './components/SavedNotice';
import { draftSlot, useAiDraft } from './hooks/useAiDraft';
import { useLeaveGuard } from './hooks/useLeaveGuard';
import type { ConfirmResult, SlotKey } from './types';

/**
 * The draft of a week for the meal planner page (T046, T053 – T055): its dishes on the calendar, where their changes
 * go, the banner over it and the question before leaving. Runs as long as the page is open.
 */
function usePlannerDraft(weekStart: string, page: AiPlannerPageContext = {}): AiPlannerDraft {
  const { draft, draftItems, change } = useAiDraft(weekStart);
  const guard = useLeaveGuard(weekStart);
  const [picking, setPicking] = useState<(SlotKey & { key: string }) | null>(null);
  const [skipped, setSkipped] = useState<(ConfirmResult & { weekStart: string }) | null>(null);

  const onDraftChange = useCallback(
    (c: DraftChange) => {
      if (c.type === 'replace' || c.type === 'choose') setPicking({ key: c.key, ...draftSlot(c.key) });
      else change(c);
    },
    [change],
  );

  const overlay = createElement(LeaveDraftDialog, { guard, count: draft?.items.length ?? 0 });
  if (!draft) {
    if (skipped?.weekStart !== weekStart) return { overlay };
    return { overlay, banner: createElement(SavedNotice, { weekStart, result: skipped, onDismiss: () => setSkipped(null) }) };
  }
  return {
    draftItems,
    onDraftChange,
    overlay,
    banner: createElement(DraftLayer, {
      weekStart,
      picking: picking && draftItems.some((i) => i.key === picking.key) ? picking : null,
      onPickingDone: () => setPicking(null),
      reload: page.reload,
      reportNetworkError: page.reportNetworkError,
      onSkipped: (result: ConfirmResult) => setSkipped({ ...result, weekStart }),
    }),
  };
}

/**
 * Plugs spec 031 into the meal planner of 030 (T046, contracts/integration.md section 3): the "Tạo bằng AI" button
 * and the draft shown on `WeekGrid`/`DayView`. Called once at start-up, before the first render.
 */
export function registerAiPlanner(): void {
  registerAiPlannerEntry(AiGenerateButton);
  registerAiPlannerDraft(usePlannerDraft);
}
