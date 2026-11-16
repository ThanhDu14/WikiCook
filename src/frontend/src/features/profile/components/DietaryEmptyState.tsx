import { Banner } from '@/shared/ui/Banner';
import { Icon } from '@/shared/ui/Icon';
import { strings } from '../strings.vi';

/** W-PF02: a user with no dietary profile yet is invited to start; nothing is required (FR-039). */
export function DietaryEmptyState({ onStart }: { onStart: () => void }) {
  return (
    <>
      <div className="wk-state">
        <div className="wk-state__art">
          <Icon name="salad" size={32} />
        </div>
        <h2 className="wk-h3">{strings.dietary.emptyTitle}</h2>
        <p>{strings.dietary.emptyBody}</p>
        <button type="button" className="wk-btn wk-btn--primary" onClick={onStart}>
          <Icon name="plus" />
          {strings.dietary.emptyStart}
        </button>
      </div>
      <Banner tone="info" icon="shield" title={strings.dietary.emptyNoteTitle}>
        {strings.dietary.emptyNoteBody}
      </Banner>
    </>
  );
}
