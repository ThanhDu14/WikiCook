import { useEffect, useState } from 'react';
import { strings } from '../strings.vi';

export interface SearchBoxProps {
  initialValue?: string;
  onSearch: (keyword: string) => void;
  className?: string;
  autoFocus?: boolean;
}

export function SearchBox({
  initialValue = '',
  onSearch,
  className = '',
  autoFocus = false,
}: SearchBoxProps) {
  const [value, setValue] = useState(initialValue);

  useEffect(() => {
    setValue(initialValue);
  }, [initialValue]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSearch(value.trim());
  };

  return (
    <form onSubmit={handleSubmit} className={`wk-search ${className}`} role="search">
      <svg className="wk-icon" viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="11" cy="11" r="8" />
        <path d="m21 21-4.3-4.3" />
      </svg>
      <input
        type="search"
        className="wk-input"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder={strings.search.searchPlaceholder}
        aria-label={strings.search.searchAriaLabel}
        autoFocus={autoFocus}
      />
    </form>
  );
}
