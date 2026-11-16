/**
 * Full-page navigation, kept in one place so tests can replace it. After sign-out the page is replaced (not pushed)
 * so Back does not show member content from history (spec 010 US1-4).
 */
export function leaveTo(path: string): void {
  window.location.replace(path);
}
