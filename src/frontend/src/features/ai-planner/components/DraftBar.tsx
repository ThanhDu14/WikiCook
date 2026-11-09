import { Icon } from '@/features/meal-planner/components/Icon';
import { strings } from '../strings.vi';
import type { DraftResponse } from '../types';
import { AiDisclaimer } from './AiDisclaimer';

/**
 * Banner over the calendar while a draft is shown (W-AI03, T045 first version): mode, counts and the disclaimer.
 * "Lưu thực đơn", "Tạo lại" and "Bỏ nháp" come with US2 (T055).
 */
export function DraftBar({ draft }: { draft: DraftResponse }) {
  const auto = draft.mode === 'AUTO';
  const title = draft.notice === 'AI_DAILY_CAP' ? strings.aiDailyCap : auto ? strings.autoNoAi : strings.draftTitleAi;
  const counts = [strings.newDishes(draft.items.length), draft.chooseSlots.length > 0 ? strings.chooseSlots(draft.chooseSlots.length) : null]
    .filter(Boolean)
    .join(' · ');
  return (
    <div className="wk-banner wk-banner--warning" role="region" aria-label={strings.draftTitleAi}>
      <Icon name="info" />
      <div className="sx-stack" style={{ gap: 4 }}>
        <div className="sx-row" style={{ gap: 8, flexWrap: 'wrap' }}>
          <span className="wk-badge wk-badge--ai">{auto ? strings.modeAuto : strings.modeAi}</span>
          <strong>{title}</strong>
        </div>
        <span>{counts}</span>
        <AiDisclaimer />
      </div>
    </div>
  );
}
