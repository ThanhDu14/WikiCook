import { useRef, type KeyboardEvent } from 'react';
import { strings } from '../strings.vi';
import { Icon } from './Icon';

interface StarRatingInputProps {
  value: number | null;
  onChange: (value: number) => void;
  labelledBy: string;
  invalid?: boolean;
  describedBy?: string;
}

/**
 * 5-star selector of W-RV02 (wk-rating-input), with no default (FR-003). It is a radio group: one tab stop, arrow
 * keys move and select, and the selected star is announced (NFR-005).
 */
export function StarRatingInput({ value, onChange, labelledBy, invalid = false, describedBy }: StarRatingInputProps) {
  const buttons = useRef<(HTMLButtonElement | null)[]>([]);

  const select = (star: number) => {
    onChange(star);
    buttons.current[star - 1]?.focus();
  };

  const onKeyDown = (event: KeyboardEvent<HTMLButtonElement>) => {
    const current = value ?? 0;
    if (event.key === 'ArrowRight' || event.key === 'ArrowUp') {
      event.preventDefault();
      select(Math.min(5, current + 1));
    } else if (event.key === 'ArrowLeft' || event.key === 'ArrowDown') {
      event.preventDefault();
      select(Math.max(1, current - 1));
    } else if (event.key === 'Home') {
      event.preventDefault();
      select(1);
    } else if (event.key === 'End') {
      event.preventDefault();
      select(5);
    }
  };

  return (
    <div
      className="wk-rating-input"
      role="radiogroup"
      aria-labelledby={labelledBy}
      aria-invalid={invalid || undefined}
      aria-describedby={describedBy}
    >
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          ref={(el) => {
            buttons.current[star - 1] = el;
          }}
          type="button"
          role="radio"
          aria-checked={value === star}
          aria-label={strings.form.star(star)}
          tabIndex={(value ?? 1) === star ? 0 : -1}
          className={value !== null && star <= value ? 'is-on' : ''}
          onClick={() => select(star)}
          onKeyDown={onKeyDown}
        >
          <Icon name="star" />
        </button>
      ))}
    </div>
  );
}
