import { useId } from 'react';

/** The ids a field exposes so a wrapper input can be wired to the same label, hint and error. */
export function useFieldIds(id?: string) {
  const generated = useId();
  const inputId = id ?? generated;
  return { inputId, helperId: `${inputId}-helper`, errorId: `${inputId}-error` };
}
