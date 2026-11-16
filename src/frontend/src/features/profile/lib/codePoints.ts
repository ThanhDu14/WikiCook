/** Length in code points, so an emoji counts as one character, as `@CodePointLength` does on the server (011 FR-002). */
export function codePointLength(value: string): number {
  return [...value].length;
}

/** The text a user typed, stripped of surrounding white space; what the server stores and counts. */
export const stripped = (value: string) => value.trim();
