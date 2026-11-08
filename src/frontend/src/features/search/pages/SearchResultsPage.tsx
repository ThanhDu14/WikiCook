import { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { searchApi } from '../api/searchApi';
import { ActiveFilterChips } from '../components/ActiveFilterChips';
import { FilterPanel } from '../components/FilterPanel';
import { FilterSheet } from '../components/FilterSheet';
import { EmptyState, ErrorState, LoadingState } from '../components/ResultStates';
import { ResultsList } from '../components/ResultsList';
import { SearchBox } from '../components/SearchBox';
import { useRecipeSearch } from '../hooks/useRecipeSearch';
import { parseSearchState, toSearchParams, updateSearchState } from '../lib/searchState';
import { strings } from '../strings.vi';
import type { SearchOptions, SearchState } from '../types';

export function SearchResultsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const searchState = parseSearchState(searchParams);
  const { data, loading, error, retry } = useRecipeSearch(searchState);

  const [options, setOptions] = useState<SearchOptions | null>(null);
  const [isFilterSheetOpen, setIsFilterSheetOpen] = useState(false);

  useEffect(() => {
    let cancelled = false;
    searchApi
      .getSearchOptions()
      .then((res) => {
        if (!cancelled) setOptions(res);
      })
      .catch(() => {});

    return () => {
      cancelled = true;
    };
  }, []);

  const handleSearch = (newKeyword: string) => {
    const next = updateSearchState(searchState, { q: newKeyword });
    setSearchParams(toSearchParams(next));
  };

  const handleApplyFilters = (appliedFilters: SearchState) => {
    const next = updateSearchState(searchState, {
      ...appliedFilters,
      q: searchState.q,
      showHidden: searchState.showHidden,
    });
    setSearchParams(toSearchParams(next));
  };

  const handleRemoveFilter = (key: keyof SearchState, value?: string) => {
    if (key === 'time' || key === 'diet') {
      const next = updateSearchState(searchState, { [key]: undefined });
      setSearchParams(toSearchParams(next));
    } else if (key === 'cuisine' || key === 'category' || key === 'difficulty' || key === 'method') {
      const current = (searchState[key] as string[] | undefined) ?? [];
      const nextArray = current.filter((item) => item !== value);
      const next = updateSearchState(searchState, {
        [key]: nextArray.length > 0 ? nextArray : undefined,
      } as Partial<SearchState>);
      setSearchParams(toSearchParams(next));
    }
  };

  const handleClearFilters = () => {
    const next = updateSearchState(
      { q: searchState.q, showHidden: searchState.showHidden },
      {
        time: undefined,
        cuisine: undefined,
        category: undefined,
        difficulty: undefined,
        method: undefined,
        diet: undefined,
      },
    );
    setSearchParams(toSearchParams(next));
  };

  return (
    <div className="sx-stack" style={{ gap: '20px', maxWidth: '1080px', margin: '0 auto' }}>
      <header className="sx-top" style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
        <Link
          to="/"
          className="wk-icon-btn wk-icon-btn--plain"
          aria-label={strings.search.backAriaLabel}
        >
          <svg className="wk-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path d="m15 18-6-6 6-6" />
          </svg>
        </Link>
        <h1 className="wk-h2" style={{ margin: 0 }}>
          {strings.search.title}
        </h1>
      </header>

      <div className="sx-row" style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
        <div className="sx-grow" style={{ flex: 1 }}>
          <SearchBox initialValue={searchState.q ?? ''} onSearch={handleSearch} />
        </div>
        <button
          className="wk-icon-btn"
          type="button"
          aria-label={strings.search.filterButtonAriaLabel}
          onClick={() => setIsFilterSheetOpen(true)}
        >
          <svg className="wk-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M21 4h-7" />
            <path d="M10 4H3" />
            <path d="M21 12h-9" />
            <path d="M8 12H3" />
            <path d="M21 20h-5" />
            <path d="M12 20H3" />
            <path d="M14 2v4" />
            <path d="M8 10v4" />
            <path d="M16 18v4" />
          </svg>
        </button>
      </div>

      {/* Active filter chips (T032) */}
      <ActiveFilterChips
        appliedFilters={data?.appliedFilters}
        state={searchState}
        onRemove={handleRemoveFilter}
        onClearAll={handleClearFilters}
      />

      <div className="wx-split wx-split--left" style={{ display: 'flex', gap: '24px' }}>
        {/* Desktop sidebar filter panel (hidden on mobile via CSS) */}
        {options && (
          <aside className="wx-aside hidden lg:block" style={{ width: '280px', flexShrink: 0 }}>
            <FilterPanel
              options={options}
              selected={searchState}
              onChange={handleApplyFilters}
            />
          </aside>
        )}

        <main className="sx-body sx-stack" style={{ gap: '16px', flex: 1, minWidth: 0 }}>
          {/* Results count & status announced via aria-live */}
          {data && (
            <div
              className="sx-between"
              style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}
            >
              <p className="wk-small" role="status" aria-live="polite" style={{ margin: 0 }}>
                <strong>{strings.search.resultsCount(data.totalItems, data.keyword)}</strong>
              </p>
            </div>
          )}

          {/* Content area: loading skeleton, error state, empty state or results list */}
          {loading && !data && <LoadingState />}

          {error && !data && <ErrorState onRetry={retry} />}

          {data && data.totalItems === 0 && (
            <EmptyState
              keyword={data.keyword || searchState.q || ''}
              onClearFilters={handleClearFilters}
            />
          )}

          {data && data.totalItems > 0 && <ResultsList items={data.items} />}
        </main>
      </div>

      {/* Mobile filter sheet (< 768px) */}
      {options && (
        <FilterSheet
          isOpen={isFilterSheetOpen}
          onClose={() => setIsFilterSheetOpen(false)}
          options={options}
          currentFilters={searchState}
          onApply={handleApplyFilters}
          totalResults={data?.totalItems ?? 0}
        />
      )}
    </div>
  );
}
