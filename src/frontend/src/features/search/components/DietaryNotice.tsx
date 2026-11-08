import { strings } from '../strings.vi';
import type { DietaryNotice as DietaryNoticeType } from '../types';

export interface DietaryNoticeProps {
  notice?: DietaryNoticeType;
  conflictingCount?: number;
  onShowHidden: () => void;
}

export function DietaryNotice({
  notice,
  conflictingCount,
  onShowHidden,
}: DietaryNoticeProps) {
  if (!notice || !notice.profileAvailable) {
    return null;
  }

  // Warning banner when showing hidden recipes (SR-06)
  if (notice.showingHidden) {
    const count = conflictingCount ?? notice.hiddenCount ?? 0;
    return (
      <div className="wk-banner wk-banner--warning" role="region" aria-label="Cảnh báo hồ sơ ăn uống">
        <svg className="wk-icon" viewBox="0 0 24 24" aria-hidden="true">
          <path d="m21.73 18-8-14a2 2 0 0 0-3.48 0l-8 14A2 2 0 0 0 4 21h16a2 2 0 0 0 1.73-3" />
          <path d="M12 9v4" />
          <path d="M12 17h.01" />
        </svg>
        <div>
          <strong>{strings.dietary.showingHiddenTitle(count)}</strong>
          {' '}
          <span>{strings.dietary.showingHiddenSubtitle}</span>
        </div>
      </div>
    );
  }

  // Info notice when recipes are hidden by profile (SR-02)
  if (notice.applied && notice.hiddenCount > 0) {
    return (
      <div className="wk-banner wk-banner--info" role="region" aria-label="Thông báo hồ sơ ăn uống">
        <svg className="wk-icon" viewBox="0 0 24 24" aria-hidden="true">
          <path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z" />
        </svg>
        <div>
          <strong>{strings.dietary.hiddenNotice(notice.hiddenCount)}</strong>
          {' '}
          <button
            className="wk-link"
            type="button"
            onClick={onShowHidden}
            style={{
              background: 'none',
              border: 'none',
              padding: 0,
              cursor: 'pointer',
              font: 'inherit',
              textDecoration: 'underline',
            }}
          >
            {strings.dietary.showForThisSearch}
          </button>
        </div>
      </div>
    );
  }

  return null;
}

export interface DietaryAllHiddenStateProps {
  hiddenCount: number;
  keyword: string;
  onShowHidden: () => void;
  onEditProfile?: () => void;
}

export function DietaryAllHiddenState({
  hiddenCount,
  keyword,
  onShowHidden,
  onEditProfile,
}: DietaryAllHiddenStateProps) {
  return (
    <div className="wk-state">
      <div className="wk-state__art">
        <svg className="wk-icon wk-icon--32" viewBox="0 0 24 24" aria-hidden="true">
          <path d="M20 13c0 5-3.5 7.5-7.66 8.95a1 1 0 0 1-.67-.01C7.5 20.5 4 18 4 13V6a1 1 0 0 1 1-1c2 0 4.5-1.2 6.24-2.72a1.17 1.17 0 0 1 1.52 0C14.51 3.81 17 5 19 5a1 1 0 0 1 1 1z" />
        </svg>
      </div>
      <h2 className="wk-h3">{strings.dietary.allHiddenTitle(hiddenCount)}</h2>
      <p>{strings.dietary.allHiddenDescription(keyword)}</p>
      <div className="sx-stack" style={{ width: '100%', gap: '8px' }}>
        <button
          className="wk-btn wk-btn--secondary wk-btn--block"
          type="button"
          onClick={onShowHidden}
          style={{ minHeight: '44px' }}
        >
          {strings.dietary.showForThisSearch}
        </button>
        {onEditProfile && (
          <button
            className="wk-btn wk-btn--ghost wk-btn--block"
            type="button"
            onClick={onEditProfile}
            style={{ minHeight: '44px' }}
          >
            {strings.dietary.editProfile}
          </button>
        )}
      </div>
    </div>
  );
}
