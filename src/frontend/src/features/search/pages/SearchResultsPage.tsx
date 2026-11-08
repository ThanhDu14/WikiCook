import { Link, useSearchParams } from 'react-router-dom';
import { EmptyState, ErrorState, LoadingState } from '../components/ResultStates';
import { ResultsList } from '../components/ResultsList';
import { SearchBox } from '../components/SearchBox';
import { useRecipeSearch } from '../hooks/useRecipeSearch';
import { parseSearchState, toSearchParams, updateSearchState } from '../lib/searchState';
import { strings } from '../strings.vi';

export function SearchResultsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const searchState = parseSearchState(searchParams);
  const { data, loading, error, retry } = useRecipeSearch(searchState);

  const handleSearch = (newKeyword: string) => {
    const next = updateSearchState(searchState, { q: newKeyword });
    setSearchParams(toSearchParams(next));
  };

  const handleClearFilters = () => {
    const next = updateSearchState(
      { q: searchState.q },
      {},
    );
    setSearchParams(toSearchParams(next));
  };

  return (
    <div className="sx-stack" style={{ gap: '20px', maxWidth: '720px', margin: '0 auto' }}>
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

      <main className="sx-body sx-stack" style={{ gap: '16px' }}>
        <div className="sx-row" style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <div className="sx-grow" style={{ flex: 1 }}>
            <SearchBox initialValue={searchState.q ?? ''} onSearch={handleSearch} />
          </div>
          <button
            className="wk-icon-btn"
            type="button"
            aria-label={strings.search.filterButtonAriaLabel}
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
  );
}
