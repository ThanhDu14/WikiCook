import { useEffect, useRef, useState } from 'react';
import { strings } from '../strings.vi';
import type { SearchOptions, SearchState } from '../types';
import { FilterPanel } from './FilterPanel';

export interface FilterSheetProps {
  isOpen: boolean;
  onClose: () => void;
  options: SearchOptions;
  currentFilters: SearchState;
  onApply: (applied: SearchState) => void;
  totalResults?: number;
}

export function FilterSheet({
  isOpen,
  onClose,
  options,
  currentFilters,
  onApply,
  totalResults = 0,
}: FilterSheetProps) {
  const [draftFilters, setDraftFilters] = useState<SearchState>(currentFilters);
  const [prevOpen, setPrevOpen] = useState(isOpen);
  const sheetRef = useRef<HTMLDivElement>(null);

  // Sync draft filters when sheet opens
  if (isOpen && !prevOpen) {
    setPrevOpen(true);
    setDraftFilters(currentFilters);
  } else if (!isOpen && prevOpen) {
    setPrevOpen(false);
  }

  // Focus sheet dialog when opened
  useEffect(() => {
    if (isOpen) {
      sheetRef.current?.focus();
    }
  }, [isOpen]);

  // Handle escape key
  useEffect(() => {
    if (!isOpen) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onClose();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const handleApply = () => {
    onApply(draftFilters);
    onClose();
  };

  return (
    <div
      className="wk-scrim wk-scrim--sheet"
      style={{
        position: 'fixed',
        inset: 0,
        zIndex: 50,
        backgroundColor: 'rgba(0,0,0,0.4)',
        display: 'flex',
        alignItems: 'flex-end',
      }}
    >
      <button
        type="button"
        data-testid="filter-sheet-backdrop"
        aria-label="Đóng bộ lọc"
        onClick={onClose}
        style={{
          position: 'absolute',
          inset: 0,
          width: '100%',
          height: '100%',
          background: 'transparent',
          border: 'none',
          padding: 0,
          cursor: 'default',
        }}
      />
      <div
        ref={sheetRef}
        tabIndex={-1}
        className="wk-sheet"
        role="dialog"
        aria-modal="true"
        aria-label={strings.filters.title}
        style={{
          position: 'relative',
          width: '100%',
          maxHeight: '85vh',
          display: 'flex',
          flexDirection: 'column',
          background: 'var(--color-surface)',
          borderTopLeftRadius: 'var(--radius-lg)',
          borderTopRightRadius: 'var(--radius-lg)',
          padding: '16px 20px 20px',
          outline: 'none',
        }}
      >
        <span
          className="wk-sheet__handle"
          style={{
            display: 'block',
            width: '40px',
            height: '4px',
            borderRadius: '2px',
            background: 'var(--color-border-strong)',
            margin: '0 auto 12px',
            flexShrink: 0,
          }}
        />

        <div style={{ overflowY: 'auto', flex: 1, paddingRight: '4px' }}>
          <FilterPanel
            options={options}
            selected={draftFilters}
            onChange={setDraftFilters}
            variant="sheet"
          />
        </div>

        <div
          className="sx-row"
          style={{
            display: 'flex',
            gap: '12px',
            marginTop: '12px',
            paddingTop: '12px',
            borderTop: '1px solid var(--color-border)',
            alignItems: 'center',
            flexShrink: 0,
          }}
        >
          <button
            className="wk-btn wk-btn--secondary"
            type="button"
            onClick={() => setDraftFilters({})}
            style={{ minHeight: '44px' }}
          >
            {strings.filters.reset}
          </button>
          <button
            className="wk-btn wk-btn--primary sx-grow"
            type="button"
            onClick={handleApply}
            style={{ flex: 1, minHeight: '44px' }}
          >
            {strings.filters.applyButton(totalResults)}
          </button>
        </div>
      </div>
    </div>
  );
}
