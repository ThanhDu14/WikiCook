import { codePointLength, stripped } from '../lib/codePoints';
import { parseWholeNumber } from '../lib/wholeNumber';

describe('codePoints (spec 011 T015)', () => {
  it('counts an emoji as one character, as @CodePointLength does on the server', () => {
    expect(codePointLength('😀')).toBe(1);
    expect(codePointLength('Bếp😀')).toBe(4);
    expect('😀'.length).toBe(2);
  });

  it('strips surrounding white space before counting', () => {
    expect(codePointLength(stripped('  Du  '))).toBe(2);
  });
});

describe('parseWholeNumber', () => {
  it('empty means not set', () => {
    expect(parseWholeNumber('  ', 1, 20)).toEqual({ value: null, valid: true });
  });

  it('accepts a whole number in range, including the ends', () => {
    expect(parseWholeNumber('1', 1, 20)).toEqual({ value: 1, valid: true });
    expect(parseWholeNumber(' 20 ', 1, 20)).toEqual({ value: 20, valid: true });
  });

  it.each(['0', '21', '-3', '2.5', 'abc', '1e2'])('refuses %s', (text) => {
    expect(parseWholeNumber(text, 1, 20).valid).toBe(false);
  });
});
