import { strings } from '../strings.vi';
import type { AppliedFilters, SearchState } from '../types';

export interface ActiveFilterChipsProps {
  appliedFilters?: AppliedFilters | null;
  state: SearchState;
  onRemove: (key: keyof SearchState, value?: string) => void;
  onClearAll: () => void;
  className?: string;
}

interface ChipItem {
  id: string;
  label: string;
  key: keyof SearchState;
  value?: string;
}

export function ActiveFilterChips({
  appliedFilters,
  state,
  onRemove,
  onClearAll,
  className = '',
}: ActiveFilterChipsProps) {
  const chips: ChipItem[] = [];

  // Time
  if (state.time) {
    chips.push({
      id: `time-${state.time}`,
      label: strings.timeRange[state.time] ?? state.time,
      key: 'time',
    });
  }

  // Cuisines
  if (state.cuisine && state.cuisine.length > 0) {
    for (const c of state.cuisine) {
      const name = appliedFilters?.cuisines.find((opt) => opt.slug === c)?.name ?? c;
      chips.push({
        id: `cuisine-${c}`,
        label: name,
        key: 'cuisine',
        value: c,
      });
    }
  }

  // Categories
  if (state.category && state.category.length > 0) {
    for (const cat of state.category) {
      const name = appliedFilters?.categories.find((opt) => opt.slug === cat)?.name ?? cat;
      chips.push({
        id: `category-${cat}`,
        label: name,
        key: 'category',
        value: cat,
      });
    }
  }

  // Difficulties
  if (state.difficulty && state.difficulty.length > 0) {
    for (const d of state.difficulty) {
      chips.push({
        id: `difficulty-${d}`,
        label: strings.difficulty[d] ?? d,
        key: 'difficulty',
        value: d,
      });
    }
  }

  // Methods
  if (state.method && state.method.length > 0) {
    for (const m of state.method) {
      const name = appliedFilters?.methods.find((opt) => opt.slug === m)?.name ?? m;
      chips.push({
        id: `method-${m}`,
        label: name,
        key: 'method',
        value: m,
      });
    }
  }

  // Diet
  if (state.diet) {
    chips.push({
      id: `diet-${state.diet}`,
      label: strings.diet[state.diet] ?? state.diet,
      key: 'diet',
    });
  }

  if (chips.length === 0) {
    return null;
  }

  return (
    <div
      className={`wk-chips ${className}`}
      style={{ display: 'flex', flexWrap: 'wrap', gap: '8px', alignItems: 'center' }}
      aria-label="Các bộ lọc đang chọn"
    >
      {chips.map((chip) => (
        <button
          key={chip.id}
          className="wk-chip wk-chip--active"
          type="button"
          onClick={() => onRemove(chip.key, chip.value)}
          aria-label={strings.filters.removeChipAriaLabel(chip.label)}
          style={{ minHeight: '36px' }}
        >
          {chip.label}
          <span className="wk-chip__remove" aria-hidden="true" style={{ marginLeft: '4px' }}>
            <svg className="wk-icon wk-icon--16" viewBox="0 0 24 24" aria-hidden="true">
              <path d="M18 6 6 18" />
              <path d="m6 6 12 12" />
            </svg>
          </span>
        </button>
      ))}

      <button
        className="wk-link"
        type="button"
        onClick={onClearAll}
        style={{
          background: 'none',
          border: 'none',
          padding: '4px 8px',
          cursor: 'pointer',
          textDecoration: 'underline',
        }}
      >
        {strings.search.clearAll}
      </button>
    </div>
  );
}
