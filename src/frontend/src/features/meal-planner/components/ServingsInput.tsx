import { vi } from '../strings.vi';
import { Icon } from './Icon';

const MIN = 1;
const MAX = 20;

/** Servings stepper of MP-02 / MP-05 (`wk-serving`): only whole numbers from 1 to 20 (FR-011, T045). */
export function ServingsInput({ value, onChange, disabled }: { value: number; onChange: (servings: number) => void; disabled?: boolean }) {
  const step = (delta: number) => {
    const next = value + delta;
    if (next >= MIN && next <= MAX) onChange(next);
  };
  return (
    <span className="wk-serving" role="group" aria-label={vi.servings}>
      <button type="button" aria-label={vi.fewer} disabled={disabled || value <= MIN} onClick={() => step(-1)}>
        <Icon name="minus" />
      </button>
      <output>{vi.people(value)}</output>
      <button type="button" aria-label={vi.more} disabled={disabled || value >= MAX} onClick={() => step(1)}>
        <Icon name="plus" />
      </button>
    </span>
  );
}
