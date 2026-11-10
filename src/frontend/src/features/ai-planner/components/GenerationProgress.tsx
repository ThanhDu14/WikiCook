import { Icon } from '@/features/meal-planner/components/Icon';
import { strings } from '../strings.vi';

/** "Đang chọn món" while the draft is being made (W-AI02, US1-4): plain words and a way to cancel. */
export function GenerationProgress({ onCancel }: { onCancel: () => void }) {
  return (
    <div className="sx-row" style={{ alignItems: 'center', gap: 20 }}>
      <div
        className="wk-state__art"
        aria-hidden="true"
        style={{ background: 'var(--color-saffron-tint)', color: 'var(--color-saffron)', width: 56, height: 56, flex: 'none' }}
      >
        <Icon name="refresh" size={32} />
      </div>
      <div className="sx-grow" role="status" aria-live="polite">
        <h2 className="wk-h2">{strings.generatingTitle}</h2>
        <p className="wk-muted">{strings.generatingText}</p>
      </div>
      <button type="button" className="wk-btn wk-btn--secondary" onClick={onCancel}>
        {strings.cancel}
      </button>
    </div>
  );
}
