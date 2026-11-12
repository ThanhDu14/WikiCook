import { strings } from '../strings.vi';
import type { SearchSort } from '../types';

export interface SortSelectProps {
  value: SearchSort;
  hasKeyword: boolean;
  onChange: (sort: SearchSort) => void;
  className?: string;
}

const ALL_SORTS: SearchSort[] = ['RELEVANCE', 'NEWEST', 'TOP_RATED', 'QUICKEST'];
const NO_KEYWORD_SORTS: SearchSort[] = ['NEWEST', 'TOP_RATED', 'QUICKEST'];

export function SortSelect({
  value,
  hasKeyword,
  onChange,
  className = '',
}: SortSelectProps) {
  const options = hasKeyword ? ALL_SORTS : NO_KEYWORD_SORTS;

  return (
    <label
      className={`wk-small wk-muted sx-row ${className}`}
      htmlFor="search-sort-select"
      style={{ display: 'flex', alignItems: 'center', gap: '8px' }}
    >
      Sắp xếp
      <select
        className="wk-select"
        id="search-sort-select"
        value={value}
        onChange={(e) => onChange(e.target.value as SearchSort)}
        style={{ minHeight: '44px', width: 'auto', padding: '0 12px' }}
      >
        {options.map((opt) => (
          <option key={opt} value={opt}>
            {strings.sort[opt]}
          </option>
        ))}
      </select>
    </label>
  );
}
