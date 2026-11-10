import type { ReactNode } from 'react';
import { addWeeks, formatWeekRange } from '../lib/week';
import { vi } from '../strings.vi';
import { Icon } from './Icon';

interface Props {
  weekStart: string;
  /** Monday of the current week in Vietnam. */
  currentWeek: string;
  onChange: (weekStart: string) => void;
  /** "Tạo bằng AI" of spec 031, when registered (FR-031). */
  aiAction?: ReactNode;
  /** Other actions of the week, such as "Đi chợ" (T081). */
  actions?: ReactNode;
  /** Phone layout of MP-01: the range between the arrows; the page shows the actions under the day. */
  compact?: boolean;
}

/**
 * Header of the week (W-MP01 `wx-head`, T073): previous and next week around the date range, "Tuần này" when
 * another week is shown (US4-1, US4-2, FR-021), and the actions of the week on the right; `compact` is MP-01.
 */
export function WeekHeader({ weekStart, currentWeek, onChange, aiAction, actions, compact = false }: Props) {
  if (compact) {
    return (
      <header className="sx-between">
        <button type="button" className="wk-icon-btn wk-icon-btn--plain" aria-label={vi.previousWeek} onClick={() => onChange(addWeeks(weekStart, -1))}>
          <Icon name="chevronLeft" />
        </button>
        <div className="sx-center">
          <h1 className="wk-h3">{formatWeekRange(weekStart)}</h1>
          {weekStart !== currentWeek && (
            <button type="button" className="wk-link wk-small" style={{ minHeight: 44 }} onClick={() => onChange(currentWeek)}>
              {vi.thisWeek}
            </button>
          )}
        </div>
        <button type="button" className="wk-icon-btn wk-icon-btn--plain" aria-label={vi.nextWeek} onClick={() => onChange(addWeeks(weekStart, 1))}>
          <Icon name="chevronRight" />
        </button>
      </header>
    );
  }
  return (
    <header className="wx-head">
      <div>
        <p className="wk-eyebrow">{vi.weekEyebrow}</p>
        <div className="sx-row" style={{ gap: 8, flexWrap: 'wrap' }}>
          <button type="button" className="wk-icon-btn" aria-label={vi.previousWeek} onClick={() => onChange(addWeeks(weekStart, -1))}>
            <Icon name="chevronLeft" />
          </button>
          <h1>{formatWeekRange(weekStart)}</h1>
          <button type="button" className="wk-icon-btn" aria-label={vi.nextWeek} onClick={() => onChange(addWeeks(weekStart, 1))}>
            <Icon name="chevronRight" />
          </button>
          {weekStart !== currentWeek && (
            <button type="button" className="wk-link wk-small" style={{ marginLeft: 8 }} onClick={() => onChange(currentWeek)}>
              {vi.thisWeek}
            </button>
          )}
        </div>
      </div>
      {(actions || aiAction) && (
        <div className="sx-row" role="group" aria-label={vi.weekActions} style={{ display: 'flex', flexWrap: 'wrap' }} /* .wx-head>div is a grid */>
          {actions}
          {aiAction}
        </div>
      )}
    </header>
  );
}
