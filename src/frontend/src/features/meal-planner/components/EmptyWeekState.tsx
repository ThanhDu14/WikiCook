import { useId, type ReactNode } from 'react';
import { vi } from '../strings.vi';
import { Icon } from './Icon';

interface Props {
  onAddDish: () => void;
  /** "Tạo bằng AI" of spec 031, when registered (FR-022, FR-031). */
  aiAction?: ReactNode;
}

/** A week without dishes (W-MP06 / MP-06 `wk-state`, T074): what to do next, by hand or with the AI planner (US4-3). */
export function EmptyWeekState({ onAddDish, aiAction }: Props) {
  const titleId = useId();
  return (
    <section className="wk-state" aria-labelledby={titleId}>
      <div className="wk-state__art">
        <Icon name="calendar" size={32} />
      </div>
      <h2 id={titleId} className="wk-h3">
        {vi.emptyWeekTitle}
      </h2>
      <p>{aiAction ? vi.emptyWeekHint : vi.emptyWeekHintManual}</p>
      <div className="sx-row" style={{ flexWrap: 'wrap', justifyContent: 'center' }}>
        {aiAction}
        <button type="button" className="wk-btn wk-btn--secondary" onClick={onAddDish}>
          <Icon name="plus" />
          {vi.addDish}
        </button>
      </div>
    </section>
  );
}
