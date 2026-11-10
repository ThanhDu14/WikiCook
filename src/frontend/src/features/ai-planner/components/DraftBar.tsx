import { Icon } from '@/features/meal-planner/components/Icon';
import { strings } from '../strings.vi';
import type { DraftResponse } from '../types';
import { AiDisclaimer } from './AiDisclaimer';

interface Props {
  draft: DraftResponse;
  /** "Lưu thực đơn" (US2-2); the buttons show only when given */
  onSave?: () => void;
  onRegenerate?: () => void;
  onDiscard?: () => void;
  saving?: boolean;
  /** why the last save failed, in Vietnamese */
  error?: string | null;
}

/**
 * Banner over the calendar while a draft is shown (W-AI03, T045, T055): mode, counts, the disclaimer and
 * "Bỏ nháp" / "Tạo lại" / "Lưu thực đơn".
 */
export function DraftBar({ draft, onSave, onRegenerate, onDiscard, saving = false, error = null }: Props) {
  const auto = draft.mode === 'AUTO';
  const title = draft.notice === 'AI_DAILY_CAP' ? strings.aiDailyCap : auto ? strings.autoNoAi : strings.draftTitleAi;
  const counts = [strings.newDishes(draft.items.length), draft.chooseSlots.length > 0 ? strings.chooseSlots(draft.chooseSlots.length) : null]
    .filter(Boolean)
    .join(' · ');
  const actions = onSave || onRegenerate || onDiscard;
  return (
    <div className="wk-banner wk-banner--warning" role="region" aria-label={strings.draftTitleAi}>
      <Icon name="info" />
      <div className="sx-stack sx-grow" style={{ gap: 4 }}>
        <div className="sx-between" style={{ gap: 8, flexWrap: 'wrap' }}>
          <div className="sx-row" style={{ gap: 8, flexWrap: 'wrap' }}>
            <span className="wk-badge wk-badge--ai">{auto ? strings.modeAuto : strings.modeAi}</span>
            <strong>{title}</strong>
          </div>
          {actions && (
            <div className="sx-row" style={{ gap: 8, flexWrap: 'wrap' }}>
              {onDiscard && (
                <button type="button" className="wk-btn wk-btn--ghost" disabled={saving} onClick={onDiscard}>
                  {strings.discard}
                </button>
              )}
              {onRegenerate && (
                <button type="button" className="wk-btn wk-btn--secondary" disabled={saving} onClick={onRegenerate}>
                  <Icon name="refresh" />
                  {strings.regenerate}
                </button>
              )}
              {onSave && (
                <button type="button" className="wk-btn wk-btn--primary" disabled={saving || draft.items.length === 0} aria-busy={saving} onClick={onSave}>
                  {saving ? strings.saving : strings.save}
                </button>
              )}
            </div>
          )}
        </div>
        <span>{counts}</span>
        <AiDisclaimer />
        {error && (
          <span role="alert" className="wk-small" style={{ color: 'var(--color-danger)' }}>
            {error}
          </span>
        )}
      </div>
    </div>
  );
}
