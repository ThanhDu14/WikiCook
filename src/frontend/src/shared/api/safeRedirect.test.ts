import { loginPathFor, safeRedirect } from './safeRedirect';

describe('safeRedirect (spec 010 FR-011)', () => {
  it('accepts a path inside WikiCook, with its query', () => {
    expect(safeRedirect('/meal-planner?week=2026-10-26')).toBe('/meal-planner?week=2026-10-26');
    expect(safeRedirect('/recipes/12?action=add-to-plan')).toBe('/recipes/12?action=add-to-plan');
  });

  it.each([
    ['another site', 'https://evil.com'],
    ['a protocol-relative URL', '//evil.com'],
    ['a backslash trick', '/\\evil.com'],
    ['a script', 'javascript:alert(1)'],
    ['a control character', '/ok\n/evil'],
    ['the sign-in page', '/login'],
    ['the sign-in page with a query', '/login?redirect=/x'],
    ['the register page', '/register'],
    ['a relative path', 'meal-planner'],
    ['nothing', ''],
  ])('goes home for %s', (_label, value) => {
    expect(safeRedirect(value)).toBe('/');
  });

  it('goes home for a missing value and honours another fallback', () => {
    expect(safeRedirect(null)).toBe('/');
    expect(safeRedirect(undefined, '/profile')).toBe('/profile');
  });

  it('builds the sign-in path the way 030 does', () => {
    expect(loginPathFor('/meal-planner', '?week=2026-10-26')).toBe(
      `/login?redirect=${encodeURIComponent('/meal-planner?week=2026-10-26')}`,
    );
  });
});
