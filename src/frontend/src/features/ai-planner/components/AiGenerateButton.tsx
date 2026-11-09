import { useEffect, useState } from 'react';
import { Icon } from '@/features/meal-planner/components/Icon';
import { aiPlannerApi } from '../api/aiPlannerApi';
import { useAiDraft } from '../hooks/useAiDraft';
import { strings } from '../strings.vi';
import type { GenerationOptions } from '../types';
import { AiGenerateDialog } from './AiGenerateDialog';

/**
 * "Tạo bằng AI" of a week (T045, FR-001), registered into 030's `aiPlannerEntry`. Hidden for a past week or when the
 * options cannot be read (NFR-006); disabled with a hint when no slot is empty; opens the dialog with fresh options.
 */
export function AiGenerateButton({ weekStart }: { weekStart: string }) {
  // options and the open dialog belong to the week they were read for: another week starts hidden and closed
  const [loaded, setLoaded] = useState<{ weekStart: string; options: GenerationOptions } | null>(null);
  const [openFor, setOpenFor] = useState<string | null>(null);
  const { setDraft } = useAiDraft(weekStart);

  useEffect(() => {
    let live = true;
    aiPlannerApi.getOptions(weekStart).then(
      (o) => live && setLoaded({ weekStart, options: o }),
      () => live && setLoaded(null),
    );
    return () => {
      live = false;
    };
  }, [weekStart]);

  const options = loaded?.weekStart === weekStart ? loaded.options : null;
  const open = openFor === weekStart;
  if (!options || options.selectableDays.length === 0) return null;
  const full = options.emptySlots.length === 0;

  const openDialog = async () => {
    // the week may have changed since the button was drawn
    const fresh = await aiPlannerApi.getOptions(weekStart).catch(() => options);
    setLoaded({ weekStart, options: fresh });
    if (fresh.selectableDays.length > 0 && fresh.emptySlots.length > 0) setOpenFor(weekStart);
  };

  return (
    <>
      <span className="sx-row" style={{ gap: 8, flexWrap: 'wrap' }}>
        <button type="button" className="wk-btn wk-btn--warm" disabled={full} aria-describedby={full ? `ai-full-${weekStart}` : undefined} onClick={() => void openDialog()}>
          <Icon name="refresh" />
          {strings.generateButton}
        </button>
        {full && (
          <span id={`ai-full-${weekStart}`} className="wk-small wk-muted">
            {strings.weekFull}
          </span>
        )}
      </span>
      {open && (
        <AiGenerateDialog
          weekStart={weekStart}
          options={options}
          onClose={() => setOpenFor(null)}
          onDraft={(draft) => {
            setDraft(draft);
            setOpenFor(null);
          }}
        />
      )}
    </>
  );
}
