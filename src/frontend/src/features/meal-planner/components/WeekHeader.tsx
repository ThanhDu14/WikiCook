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
}

/**
 * Header of the week (W-MP01 `wx-head`, T073): previous and next week around the date range, "Tuần này" when
 * another week is shown (US4-1, US4-2, FR-021), and the actions of the week on the right.
 */
export function WeekHeader({ weekStart, currentWeek, onChange, aiAction, actions }: Props) {
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
        <div className="sx-row" role="group" aria-label={vi.weekActions} style={{ flexWrap: 'wrap' }}>
          {actions}
          {aiAction}
        </div>
      )}
    </header>
  );
}
