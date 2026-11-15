import { useId } from 'react';
import { DIET_LABELS, strings } from '../strings.vi';
import type { DietType } from '../types';

interface Props {
  options: DietType[];
  value: DietType;
  onChange: (value: DietType) => void;
}

/** Diet type from the list the server sends (no Keto until the schema has it, US1-4). */
export function DietTypeSelect({ options, value, onChange }: Props) {
  const id = useId();
  return (
    <div className="wk-field">
      <label className="wk-label" htmlFor={id}>
        {strings.dietary.dietType}
      </label>
      <select className="wk-select" id={id} value={value} onChange={(event) => onChange(event.target.value as DietType)}>
        {options.map((type) => (
          <option key={type} value={type}>
            {DIET_LABELS[type] ?? type}
          </option>
        ))}
      </select>
    </div>
  );
}
