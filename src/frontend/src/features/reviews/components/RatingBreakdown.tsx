import { formatAverage, starPercent } from '../lib/format';
import { strings } from '../strings.vi';
import type { RatingSummary, StarValue } from '../types';
import { Icon, Stars } from './Icon';

interface RatingBreakdownProps {
  summary: RatingSummary;
  selectedStar: StarValue | null;
  /** A bar filters the list by its star (`rating` query); pressing the active bar clears it. */
  onSelectStar: (star: StarValue | null) => void;
}

const STARS: StarValue[] = [5, 4, 3, 2, 1];

/**
 * Rating summary of W-RV01 (T053, FR-014): average score, stars, review count and the 5..1 distribution bars.
 * Without reviews it is the "Chưa có điểm" panel of W-RV06.
 */
export function RatingBreakdown({ summary, selectedStar, onSelectStar }: RatingBreakdownProps) {
  if (summary.totalReviews === 0) {
    return (
      <div className="wx-panel sx-stack" style={{ gap: 8 }}>
        <strong>{strings.summary.noScore}</strong>
        <Stars filled={0} />
        <p className="wk-small wk-muted">{strings.summary.noScoreHint}</p>
      </div>
    );
  }

  const average = formatAverage(summary.averageRating);
  return (
    <div className="wk-summary" style={{ flexDirection: 'column', alignItems: 'stretch' }}>
      <div className="wk-summary__score">
        <strong>{average}</strong>
        <Stars filled={summary.averageRating} label={strings.summary.starsLabel(average)} />
        <span className="wk-small wk-muted">{strings.summary.reviewCount(summary.totalReviews)}</span>
      </div>
      <div className="wk-bars">
        {STARS.map((star) => {
          const percent = starPercent(summary, star);
          const active = selectedStar === star;
          return (
            <button
              key={star}
              type="button"
              className="wk-bar"
              aria-label={strings.summary.filterByStar(star)}
              aria-pressed={active}
              title={strings.summary.barLabel(star, percent)}
              onClick={() => onSelectStar(active ? null : star)}
              style={{
                border: 0,
                background: active ? 'var(--color-saffron-tint)' : 'none',
                borderRadius: 8,
                padding: '6px 8px',
                minHeight: 44,
                font: 'inherit',
                cursor: 'pointer',
                width: '100%',
              }}
            >
              <span className="sx-row" style={{ gap: 2 }}>
                {star}
                <Icon name="star" />
              </span>
              <span className="wk-bar__track">
                <span className="wk-bar__fill" style={{ display: 'block', width: `${percent}%` }} />
              </span>
              <span>{percent}%</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
