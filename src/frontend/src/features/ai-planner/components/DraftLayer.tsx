import { useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { RecipePicker } from '@/features/meal-planner/components/RecipePicker';
import { aiPlannerApi, AiPlannerApiError } from '../api/aiPlannerApi';
import { clearAiDraft, toConfirmRequest, useAiDraft } from '../hooks/useAiDraft';
import { shoppingImportPath } from '../lib/paths';
import { strings } from '../strings.vi';
import type { ConfirmResult, GenerationOptions, SlotKey } from '../types';
import { AiGenerateDialog } from './AiGenerateDialog';
import { DraftBar } from './DraftBar';

interface Props {
  weekStart: string;
  /** the meal of the draft whose dish the picker replaces or fills, or null */
  picking: SlotKey & { key: string } | null;
  onPickingDone: () => void;
  /** 030 reads the week again after saving */
  reload?: () => void;
  /** 030 shows its offline notice */
  reportNetworkError?: () => void;
  /** saved with some dishes skipped: the page says so instead of leaving at once (US2-6) */
  onSkipped: (result: ConfirmResult) => void;
}

/**
 * Everything over the calendar while a draft is open (T053, T055): `DraftBar` with "Bỏ nháp" / "Tạo lại" /
 * "Lưu thực đơn", the picker of 030 for replacing a dish or filling "Chọn món" (FR-040), and the dialog of "Tạo lại".
 */
export function DraftLayer({ weekStart, picking, onPickingDone, reload, reportNetworkError, onSkipped }: Props) {
  const { draft, request, setDraft, clear, place } = useAiDraft(weekStart);
  const navigate = useNavigate();
  const location = useLocation();
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [regenerate, setRegenerate] = useState<GenerationOptions | null>(null);
  if (!draft) return null;

  const save = async () => {
    setSaving(true);
    setError(null);
    try {
      const result = await aiPlannerApi.confirm(weekStart, toConfirmRequest(draft));
      clear();
      reload?.();
      if (result.skippedOccupied + result.skippedUnavailable > 0) onSkipped(result);
      else navigate(shoppingImportPath(weekStart));
    } catch (e) {
      const code = e instanceof AiPlannerApiError ? e.code : 'UNKNOWN';
      if (code === 'UNAUTHENTICATED' || (e instanceof AiPlannerApiError && e.status === 401)) {
        // an expired session loses the draft; sign-in comes back to the same week (edge case)
        clearAiDraft(weekStart);
        navigate(`/login?redirect=${encodeURIComponent(location.pathname + location.search)}`, { replace: true });
        return;
      }
      if (code === 'NETWORK') {
        clearAiDraft(weekStart);
        reportNetworkError?.();
        return;
      }
      setError(strings.errors[code]);
      setSaving(false);
    }
  };

  const openRegenerate = async () => {
    try {
      const options = await aiPlannerApi.getOptions(weekStart);
      if (options.selectableDays.length > 0 && options.emptySlots.length > 0) setRegenerate(options);
      else setError(strings.errors.NO_EMPTY_SLOT);
    } catch (e) {
      setError(strings.errors[e instanceof AiPlannerApiError ? e.code : 'UNKNOWN']);
    }
  };

  return (
    <>
      <DraftBar draft={draft} saving={saving} error={error} onDiscard={clear} onRegenerate={() => void openRegenerate()} onSave={() => void save()} />
      {picking && (
        <RecipePicker
          day={picking.day}
          meal={picking.meal}
          onClose={onPickingDone}
          onPick={async (recipe) => {
            place(picking.key, recipe);
            onPickingDone();
            return null;
          }}
        />
      )}
      {regenerate && (
        <AiGenerateDialog
          weekStart={weekStart}
          options={regenerate}
          initial={request}
          onClose={() => setRegenerate(null)}
          onDraft={(next, nextRequest) => {
            setDraft(next, nextRequest);
            setRegenerate(null);
          }}
        />
      )}
    </>
  );
}
