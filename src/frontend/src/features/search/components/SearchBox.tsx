import { useState } from 'react';
import { strings } from '../strings.vi';

export interface SearchBoxProps {
  initialValue?: string;
  onSearch: (keyword: string) => void;
  className?: string;
  inputClassName?: string;
}

export function SearchBox({
  initialValue = '',
  onSearch,
  className = '',
  inputClassName = '',
}: SearchBoxProps) {
  const [value, setValue] = useState(initialValue);
  const [prevInitialValue, setPrevInitialValue] = useState(initialValue);

  if (initialValue !== prevInitialValue) {
    setPrevInitialValue(initialValue);
    setValue(initialValue);
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSearch(value.trim());
  };

  return (
    <form onSubmit={handleSubmit} className={`wk-search ${className}`.trim()} role="search">
      <svg className="wk-icon" viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="11" cy="11" r="8" />
        <path d="m21 21-4.3-4.3" />
      </svg>
      <input
        type="search"
        className={`wk-input ${inputClassName}`.trim()}
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder={strings.search.searchPlaceholder}
        aria-label={strings.search.searchAriaLabel}
      />
    </form>
  );
}
