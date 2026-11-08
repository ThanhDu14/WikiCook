import { useNavigate } from 'react-router-dom';
import { toSearchParams } from '../lib/searchState';
import { strings } from '../strings.vi';
import type { SearchState } from '../types';

export interface QuickFiltersProps {
  onSelect?: (filter: Partial<SearchState>) => void;
  className?: string;
}

export function QuickFilters({ onSelect, className = '' }: QuickFiltersProps) {
  const navigate = useNavigate();

  const handleFilterClick = (filter: Partial<SearchState>) => {
    if (onSelect) {
      onSelect(filter);
    } else {
      const params = toSearchParams(filter);
      navigate(`/search?${params.toString()}`);
    }
  };

  return (
    <div
      className={`wk-chips ${className}`}
      style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}
      role="toolbar"
      aria-label="Bộ lọc nhanh"
    >
      <button
        className="wk-chip"
        type="button"
        onClick={() => handleFilterClick({ time: 'LE30' })}
        style={{ minHeight: '44px' }}
      >
        <svg className="wk-icon wk-icon--16" viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="12" cy="12" r="10" />
          <path d="M12 6v6l4 2" />
        </svg>
        {strings.home.quickFilters.under30}
      </button>

      <button
        className="wk-chip"
        type="button"
        onClick={() => handleFilterClick({ diet: 'VEGETARIAN' })}
        style={{ minHeight: '44px' }}
      >
        <svg className="wk-icon wk-icon--16" viewBox="0 0 24 24" aria-hidden="true">
          <path d="M7 21h10" />
          <path d="M12 21a9 9 0 0 0 9-9H3a9 9 0 0 0 9 9Z" />
          <path d="M11.38 12a2.4 2.4 0 0 1-.4-4.77 2.4 2.4 0 0 1 3.2-2.77 2.4 2.4 0 0 1 3.47-.63 2.4 2.4 0 0 1 3.37 3.37 2.4 2.4 0 0 1-1.1 3.7 2.51 2.51 0 0 1 .03 1.1" />
          <path d="m13 12 4-4" />
        </svg>
        {strings.home.quickFilters.vegetarian}
      </button>

      <button
        className="wk-chip"
        type="button"
        onClick={() => handleFilterClick({ difficulty: ['EASY'] })}
        style={{ minHeight: '44px' }}
      >
        <svg className="wk-icon wk-icon--16" viewBox="0 0 24 24" aria-hidden="true">
          <path d="M18 20V10" />
          <path d="M12 20V4" />
          <path d="M6 20v-6" />
        </svg>
        {strings.home.quickFilters.easy}
      </button>
    </div>
  );
}

