import { Icon } from '@/shared/ui/Icon';
import { strings } from '../strings.vi';
import type { Allergen } from '../types';

interface Props {
  options: Allergen[];
  selected: ReadonlySet<number>;
  onToggle: (id: number) => void;
}

/**
 * Allergies are picked from the fixed list of the catalogue (US1-2, FR-009). The note below is always there: WikiCook's
 * data may be incomplete and never says a dish is safe (FR-010a).
 */
export function AllergenPicker({ options, selected, onToggle }: Props) {
  return (
    <fieldset className="wk-field" style={{ border: 0, padding: 0, margin: 0 }}>
      <legend className="wk-label">{strings.dietary.allergens}</legend>
      <div className="wk-chips">
        {options.map((allergen) => {
          const active = selected.has(allergen.id);
          return (
            <button
              key={allergen.id}
              type="button"
              className={active ? 'wk-chip wk-chip--active' : 'wk-chip'}
              aria-pressed={active}
              onClick={() => onToggle(allergen.id)}
            >
              {active ? <Icon name="check" size={16} /> : null}
              {allergen.nameVi}
            </button>
          );
        })}
      </div>
      <p className="wk-helper">
        <span>{strings.dietary.allergenNote}</span>
      </p>
    </fieldset>
  );
}
