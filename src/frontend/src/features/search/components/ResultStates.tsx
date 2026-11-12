import { strings } from '../strings.vi';
import { QuickFilters } from './QuickFilters';

export function LoadingState() {
  return (
    <div
      className="sx-card sx-stack"
      data-testid="loading-skeleton"
      aria-busy="true"
      aria-label={strings.loadingState.loading}
      style={{ gap: '12px', padding: '16px' }}
    >
      <div className="wk-skel" style={{ height: '140px', borderRadius: '12px' }} />
      <div className="wk-skel" style={{ height: '20px', width: '70%', borderRadius: '4px' }} />
      <div className="wk-skel" style={{ height: '16px', width: '50%', borderRadius: '4px' }} />
    </div>
  );
}

export interface EmptyStateProps {
  keyword: string;
  lastFilterLabel?: string;
  onClearLastFilter?: () => void;
  onClearFilters?: () => void;
}

export function EmptyState({
  keyword,
  lastFilterLabel,
  onClearLastFilter,
  onClearFilters,
}: EmptyStateProps) {
  return (
    <div className="sx-stack" style={{ gap: '16px' }}>
      <div className="wk-state">
        <div className="wk-state__art">
          <svg className="wk-icon wk-icon--32" viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="11" cy="11" r="8" />
            <path d="m21 21-4.3-4.3" />
          </svg>
        </div>
        <h2 className="wk-h3">{strings.emptyState.noResultsTitle(keyword)}</h2>
        <p>{strings.emptyState.suggestions}</p>
        {(onClearLastFilter || onClearFilters) && (
          <div className="sx-stack" style={{ width: '100%', gap: '8px' }}>
            {lastFilterLabel && onClearLastFilter && (
              <button
                className="wk-btn wk-btn--secondary wk-btn--block"
                type="button"
                onClick={onClearLastFilter}
              >
                {strings.emptyState.clearLastFilter(lastFilterLabel)}
              </button>
            )}
            {onClearFilters && (
              <button
                className="wk-btn wk-btn--ghost wk-btn--block"
                type="button"
                onClick={onClearFilters}
              >
                {strings.emptyState.clearAllFilters}
              </button>
            )}
          </div>
        )}
      </div>
      <div className="sx-stack" style={{ gap: '8px' }}>
        <p className="wk-label" style={{ margin: 0 }}>{strings.emptyState.tryQuick}</p>
        <QuickFilters />
      </div>
    </div>
  );
}

export interface ErrorStateProps {
  onRetry: () => void;
}

export function ErrorState({ onRetry }: ErrorStateProps) {
  return (
    <div className="wk-state wk-state--error">
      <div className="wk-state__art">
        <svg className="wk-icon wk-icon--32" viewBox="0 0 24 24" aria-hidden="true">
          <path d="M12 20h.01" />
          <path d="M8.5 16.429a5 5 0 0 1 7 0" />
          <path d="M5 12.859a10 10 0 0 1 5.17-2.69" />
          <path d="M19 12.859a10 10 0 0 0-2.007-1.523" />
          <path d="M2 8.82a15 15 0 0 1 4.177-2.643" />
          <path d="M22 8.82a15 15 0 0 0-11.288-3.764" />
          <path d="m2 2 20 20" />
        </svg>
      </div>
      <h2 className="wk-h3">{strings.errorState.title}</h2>
      <p>{strings.errorState.subtitle}</p>
      <button className="wk-btn wk-btn--primary" type="button" onClick={onRetry}>
        <svg className="wk-icon" viewBox="0 0 24 24" aria-hidden="true">
          <path d="M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8" />
          <path d="M21 3v5h-5" />
          <path d="M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16" />
          <path d="M8 16H3v5" />
        </svg>
        {strings.errorState.retry}
      </button>
    </div>
  );
}
