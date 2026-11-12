import {
  parseSearchState,
  toSearchParams,
  toSearchString,
  updateSearchState,
} from './searchState';
import type { SearchState } from '../types';

describe('searchState (T023)', () => {
  describe('parseSearchState', () => {
    it('parses valid parameters correctly', () => {
      const search =
        '?q=ga%20kho&time=LE30&cuisine=viet-nam&cuisine=nhat-ban&category=mon-chinh&difficulty=EASY&difficulty=MEDIUM&method=chien&diet=VEGETARIAN&sort=NEWEST&page=2&showHidden=true';
      const state = parseSearchState(search);

      expect(state).toEqual({
        q: 'ga kho',
        time: 'LE30',
        cuisine: ['viet-nam', 'nhat-ban'],
        category: ['mon-chinh'],
        difficulty: ['EASY', 'MEDIUM'],
        method: ['chien'],
        diet: 'VEGETARIAN',
        sort: 'NEWEST',
        page: 2,
        showHidden: true,
      });
    });

    it('trims whitespace from keyword q', () => {
      const state = parseSearchState('?q=%20%20ga%20kho%20%20');
      expect(state.q).toBe('ga kho');
    });

    it('ignores unknown or invalid values without throwing', () => {
      const search =
        '?q=ga&time=INVALID_TIME&difficulty=EASY&difficulty=SUPER_HARD&diet=PALEO&sort=RANDOM&page=abc&showHidden=maybe';
      const state = parseSearchState(search);

      expect(state.time).toBeUndefined();
      expect(state.difficulty).toEqual(['EASY']);
      expect(state.diet).toBeUndefined();
      expect(state.sort).toBeUndefined();
      expect(state.page).toBeUndefined();
      expect(state.showHidden).toBeUndefined();
    });

    it('ignores page < 1', () => {
      const state = parseSearchState('?page=-1');
      expect(state.page).toBeUndefined();
    });

    it('handles empty query string', () => {
      const state = parseSearchState('');
      expect(state).toEqual({});
    });
  });

  describe('toSearchParams & toSearchString', () => {
    it('serializes state into URLSearchParams with repeatable params', () => {
      const state: SearchState = {
        q: 'pho',
        time: 'LE15',
        cuisine: ['viet-nam', 'nhat-ban'],
        difficulty: ['EASY'],
        page: 3,
        showHidden: true,
      };

      const params = toSearchParams(state);
      expect(params.get('q')).toBe('pho');
      expect(params.get('time')).toBe('LE15');
      expect(params.getAll('cuisine')).toEqual(['viet-nam', 'nhat-ban']);
      expect(params.getAll('difficulty')).toEqual(['EASY']);
      expect(params.get('page')).toBe('3');
      expect(params.get('showHidden')).toBe('true');
    });

    it('omits defaults and empty values', () => {
      const state: SearchState = {
        q: '',
        cuisine: [],
        page: 1,
        showHidden: false,
      };

      const params = toSearchParams(state);
      expect(params.has('q')).toBe(false);
      expect(params.has('cuisine')).toBe(false);
      expect(params.has('page')).toBe(false);
      expect(params.has('showHidden')).toBe(false);
      expect(toSearchString(state)).toBe('');
    });

    it('round-trips state correctly', () => {
      const original: SearchState = {
        q: 'chao ga',
        time: 'LE60',
        cuisine: ['viet-nam'],
        category: ['canh-sup'],
        difficulty: ['HARD'],
        method: ['luoc'],
        diet: 'VEGAN',
        sort: 'TOP_RATED',
        page: 2,
        showHidden: true,
      };

      const roundTrip = parseSearchState(toSearchParams(original));
      expect(roundTrip).toEqual(original);
    });
  });

  describe('updateSearchState transitions', () => {
    const initialState: SearchState = {
      q: 'ga',
      time: 'LE30',
      cuisine: ['viet-nam'],
      page: 3,
      showHidden: true,
      sort: 'NEWEST',
    };

    it('changing q drops page and drops showHidden (FR-017, FR-020)', () => {
      const updated = updateSearchState(initialState, { q: 'bo' });

      expect(updated.q).toBe('bo');
      expect(updated.page).toBeUndefined();
      expect(updated.showHidden).toBeUndefined();
      // other filters kept
      expect(updated.time).toBe('LE30');
      expect(updated.cuisine).toEqual(['viet-nam']);
      expect(updated.sort).toBe('NEWEST');
    });

    it('changing a filter drops page but retains showHidden and q', () => {
      const updated = updateSearchState(initialState, { time: 'LE60' });

      expect(updated.page).toBeUndefined();
      expect(updated.showHidden).toBe(true);
      expect(updated.time).toBe('LE60');
      expect(updated.q).toBe('ga');
    });

    it('changing sort drops page but retains showHidden', () => {
      const updated = updateSearchState(initialState, { sort: 'QUICKEST' });

      expect(updated.page).toBeUndefined();
      expect(updated.showHidden).toBe(true);
      expect(updated.sort).toBe('QUICKEST');
    });

    it('changing page alone preserves other state', () => {
      const updated = updateSearchState(initialState, { page: 4 });

      expect(updated.page).toBe(4);
      expect(updated.q).toBe('ga');
      expect(updated.showHidden).toBe(true);
      expect(updated.time).toBe('LE30');
    });
  });
});
