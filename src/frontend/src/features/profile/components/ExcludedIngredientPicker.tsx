import { useId, useState } from 'react';
import { Icon } from '@/shared/ui/Icon';
import { useIngredientSuggestions } from '../hooks/useIngredientSuggestions';
import { strings } from '../strings.vi';
import { MAX_EXCLUDED_INGREDIENTS, type NamedItem } from '../types';

interface Props {
  selected: NamedItem[];
  onAdd: (item: NamedItem) => void;
  onRemove: (id: number) => void;
}

/**
 * Ingredients the user never eats (US1-3): type two letters, pick from the catalogue's suggestions (accent-insensitive,
 * so "ngo" finds "Ngò rí"); nothing outside the catalogue can be added. At 50 the field stops with an explanation
 * (Edge: 51st ingredient). Suggestions are buttons in the tab order, so it works with the keyboard (NFR-006).
 */
export function ExcludedIngredientPicker({ selected, onAdd, onRemove }: Props) {
  const inputId = useId();
  const listId = useId();
  const [query, setQuery] = useState('');
  const { items, loading, failed } = useIngredientSuggestions(query);
  const full = selected.length >= MAX_EXCLUDED_INGREDIENTS;
  const chosen = new Set(selected.map((item) => item.id));
  const options = items.filter((item) => !chosen.has(item.id));
  const searching = query.trim().length >= 2;

  function add(item: NamedItem) {
    onAdd(item);
    setQuery('');
  }

  return (
    <div className="wk-field" style={{ maxWidth: 560 }}>
      <label className="wk-label" htmlFor={inputId}>
        {strings.dietary.excluded}
      </label>
      <div className="wk-search">
        <Icon name="search" />
        <input
          className="wk-input"
          id={inputId}
          value={query}
          disabled={full}
          autoComplete="off"
          aria-controls={listId}
          onChange={(event) => setQuery(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Escape') setQuery('');
          }}
        />
      </div>
      <div id={listId} aria-label={strings.dietary.suggestionsLabel} role="group">
        {searching && !loading && !failed && options.length === 0 ? <p className="wk-small wk-muted">{strings.dietary.excludedNone}</p> : null}
        {searching && loading ? (
          <div className="sx-stack" style={{ gap: 4 }} aria-busy="true">
            <p className="wk-small wk-muted" role="status">
              {strings.dietary.excludedSearching}
            </p>
            <div className="wk-skel" style={{ height: 48 }} aria-hidden="true" />
          </div>
        ) : null}
        {failed ? (
          <p className="wk-error" role="alert">
            {strings.errors.network}
          </p>
        ) : null}
        {options.length > 0 ? (
          <div className="sx-list" style={{ marginTop: -4 }}>
            {options.map((item) => (
              <button key={item.id} type="button" onClick={() => add(item)}>
                {item.name}
              </button>
            ))}
          </div>
        ) : null}
      </div>
      {selected.length > 0 ? (
        <ul className="wk-chips" style={{ listStyle: 'none', padding: 0, margin: 0 }}>
          {selected.map((item) => (
            <li key={item.id}>
              <button type="button" className="wk-chip wk-chip--ingredient" aria-label={strings.dietary.remove(item.name)} onClick={() => onRemove(item.id)}>
                {item.name}
                <span className="wk-chip__remove" aria-hidden="true">
                  <Icon name="close" size={16} />
                </span>
              </button>
            </li>
          ))}
        </ul>
      ) : null}
      {full ? (
        <p className="wk-error" role="alert">
          {strings.dietary.excludedLimit}
        </p>
      ) : null}
      <p className="wk-helper">
        <span>{strings.dietary.excludedHelper}</span>
        <span>
          {selected.length}/{MAX_EXCLUDED_INGREDIENTS}
        </span>
      </p>
    </div>
  );
}
