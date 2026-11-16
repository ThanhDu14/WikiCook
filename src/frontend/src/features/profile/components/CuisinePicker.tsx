import { strings } from '../strings.vi';
import type { NamedItem } from '../types';

interface Props {
  options: NamedItem[];
  selected: ReadonlySet<number>;
  onToggle: (id: number) => void;
}

/** Favourite cuisines, listed by the server (the active ones), never fixed in the client (US1-4, FR-013). */
export function CuisinePicker({ options, selected, onToggle }: Props) {
  return (
    <fieldset className="wk-field wx-full" style={{ border: 0, padding: 0, margin: 0 }}>
      <legend className="wk-label">{strings.dietary.cuisines}</legend>
      <div className="wk-chips">
        {options.map((cuisine) => {
          const active = selected.has(cuisine.id);
          return (
            <button
              key={cuisine.id}
              type="button"
              className={active ? 'wk-chip wk-chip--active' : 'wk-chip'}
              aria-pressed={active}
              onClick={() => onToggle(cuisine.id)}
            >
              {cuisine.name}
            </button>
          );
        })}
      </div>
    </fieldset>
  );
}
