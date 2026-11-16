/** Parses the text of a number field: empty is "not set" (`null`); anything else must be a whole number in range. */
export function parseWholeNumber(text: string, min: number, max: number): { value: number | null; valid: boolean } {
  const trimmed = text.trim();
  if (trimmed === '') return { value: null, valid: true };
  if (!/^\d+$/.test(trimmed)) return { value: null, valid: false };
  const value = Number(trimmed);
  return value >= min && value <= max ? { value, valid: true } : { value: null, valid: false };
}
