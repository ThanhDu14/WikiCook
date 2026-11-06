import { useEffect, useState, type ReactNode } from 'react';
import { mealPlanApi } from '../api/mealPlanApi';
import { vi } from '../strings.vi';
import type { Day, ErrorCode, Meal, PickerResult, PickerScope } from '../types';
import { DishPhoto } from './DishPhoto';
import { Icon } from './Icon';
import { Overlay } from './Overlay';
import { dishName } from '../lib/dishName';

interface Props {
  day: Day;
  meal: Meal;
  /** Resolves with null when added, or the error code (422 → the list reloads). */
  onPick: (recipe: PickerResult) => Promise<ErrorCode | null>;
  onClose: () => void;
  /** "Gợi ý cho bạn" of spec 031 (FR-007c), shown above the results. */
  suggestionsSlot?: ReactNode;
}

const DEBOUNCE_MS = 300;

function meta(result: PickerResult) {
  return [result.totalTimeMinutes ? vi.minutes(result.totalTimeMinutes) : null, result.cuisineName]
    .filter(Boolean)
    .join(' · ');
}

/** Recipe picker of a slot (MP-04, T047): search, scope, ranked results, conflict warnings (FR-007 – FR-007c). */
export function RecipePicker({ day, meal, onPick, onClose, suggestionsSlot }: Props) {
  const [typed, setTyped] = useState('');
  const [q, setQ] = useState('');
  const [scope, setScope] = useState<PickerScope>('ALL');
  const [page, setPage] = useState(0);
  const [results, setResults] = useState<PickerResult[]>([]);
  const [hasNext, setHasNext] = useState(false);
  const [reloadKey, setReloadKey] = useState(0);
  const [loadedKey, setLoadedKey] = useState<string | null>(null);
  const requestKey = `${meal}|${q}|${scope}|${page}|${reloadKey}`;
  const loading = loadedKey !== requestKey;
  const [message, setMessage] = useState<string | null>(null);
  const [confirming, setConfirming] = useState<PickerResult | null>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => {
      setQ(typed.trim());
      setPage(0);
    }, DEBOUNCE_MS);
    return () => clearTimeout(timer);
  }, [typed]);

  useEffect(() => {
    let current = true;
    mealPlanApi
      .searchPickerRecipes({ meal, q: q || undefined, scope, page })
      .then((found) => {
        if (!current) return;
        setResults((previous) => (page === 0 ? found.content : [...previous, ...found.content]));
        setHasNext(found.hasNext);
      })
      .catch(() => current && setMessage(vi.loadFailed))
      .finally(() => current && setLoadedKey(requestKey));
    return () => {
      current = false;
    };
  }, [meal, q, scope, page, requestKey]);

  const pick = async (result: PickerResult) => {
    setConfirming(null);
    setBusy(true);
    const code = await onPick(result);
    setBusy(false);
    if (code === 'RECIPE_NOT_AVAILABLE') {
      setMessage(vi.errors.RECIPE_NOT_AVAILABLE);
      setPage(0);
      setReloadKey((k) => k + 1);
    }
  };

  const choose = (result: PickerResult) => (result.conflicts.length > 0 ? setConfirming(result) : void pick(result));
  const title = vi.pickerTitle(vi.meal[meal], vi.dayShort[day]);
  const switchScope = (next: PickerScope) => {
    setScope(next);
    setPage(0);
  };

  return (
    // A compact `wx-modal`: title, search and scope stay put, only the list scrolls.
    <Overlay
      title={title}
      onClose={onClose}
      variant="modal"
      panelStyle={{ width: 'min(520px, 100%)', height: 'min(640px, 100%)', padding: 20, gap: 12, overflow: 'hidden' }}
    >
      {(titleId) => (
        <>
          <div className="wx-modal__head">
            <h2 id={titleId} className="wk-h3">
              {title}
            </h2>
            <button type="button" className="wk-icon-btn wk-icon-btn--plain" aria-label={vi.close} onClick={onClose}>
              <Icon name="close" />
            </button>
          </div>
          <div className="grid gap-ds-3">
            <div className="wk-search">
              <Icon name="search" />
              <input
                type="search"
                className="wk-input"
                placeholder={vi.searchPlaceholder}
                aria-label={vi.search}
                value={typed}
                maxLength={100}
                data-autofocus
                onChange={(event) => setTyped(event.target.value)}
              />
            </div>
            <div className="wk-segmented self-start" role="group" aria-label={vi.scopeLabel}>
              <button type="button" aria-pressed={scope === 'ALL'} onClick={() => switchScope('ALL')}>
                {vi.scopeAll}
              </button>
              <button type="button" aria-pressed={scope === 'COLLECTIONS'} onClick={() => switchScope('COLLECTIONS')}>
                {vi.scopeCollections}
              </button>
            </div>
          </div>
          <div className="grid min-h-0 flex-1 content-start gap-ds-3 overflow-y-auto" style={{ overscrollBehavior: 'contain' }}>
            {message && (
              <div className="wk-banner wk-banner--danger" role="alert">
                <Icon name="warning" />
                <div>{message}</div>
              </div>
            )}
            {suggestionsSlot && <div data-testid="picker-suggestions">{suggestionsSlot}</div>}
            {!loading && results.length === 0 ? (
              <div className="wk-state">
                <div className="wk-state__art">
                  <Icon name="search" size={32} />
                </div>
                <p>{scope === 'COLLECTIONS' && !q ? vi.noCollections : vi.noResults}</p>
              </div>
            ) : (
              <ul className="grid gap-ds-2" aria-busy={loading}>
                {results.map((result) => (
                  <li key={result.recipeId} className="wk-card wk-card--compact">
                    <DishPhoto url={result.coverImageUrl} name={result.title} size={48} />
                    <div className="wk-card__body">
                      <span className="wk-card__title" title={result.title}>
                        {dishName(result.title)}
                      </span>
                      <span className="wk-small wk-muted">{meta(result)}</span>
                      {result.conflicts.length > 0 && (
                        <span className="wk-chips">
                          {result.conflicts.map((c) => (
                            <span key={`${c.type}-${c.label}`} className="wk-chip wk-chip--allergen">
                              {vi.conflict[c.type](c.label)}
                            </span>
                          ))}
                        </span>
                      )}
                    </div>
                    <button type="button" className="wk-btn wk-btn--primary wk-btn--sm" disabled={busy} onClick={() => choose(result)}>
                      {vi.choose}
                    </button>
                  </li>
                ))}
              </ul>
            )}
            {hasNext && (
              <button type="button" className="wk-btn wk-btn--secondary self-center" disabled={loading} onClick={() => setPage((p) => p + 1)}>
                {vi.loadMore}
              </button>
            )}
          </div>
          {confirming && (
            <Overlay title={vi.conflictTitle} onClose={() => setConfirming(null)} variant="dialog" role="alertdialog">
              {() => (
                <>
                  <h2 className="wk-h3">{vi.conflictTitle}</h2>
                  <p>{confirming.title}</p>
                  <ul className="wk-chips">
                    {confirming.conflicts.map((c) => (
                      <li key={`${c.type}-${c.label}`} className="wk-chip wk-chip--allergen">
                        {vi.conflict[c.type](c.label)}
                      </li>
                    ))}
                  </ul>
                  <div className="wk-dialog__actions">
                    <button type="button" className="wk-btn wk-btn--ghost" onClick={() => setConfirming(null)}>
                      {vi.cancel}
                    </button>
                    <button type="button" className="wk-btn wk-btn--warm" onClick={() => void pick(confirming)}>
                      {vi.conflictConfirm}
                    </button>
                  </div>
                </>
              )}
            </Overlay>
          )}
        </>
      )}
    </Overlay>
  );
}
