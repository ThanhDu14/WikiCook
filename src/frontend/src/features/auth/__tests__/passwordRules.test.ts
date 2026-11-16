import { checkPassword, isPasswordValid, passwordLength } from '../lib/passwordRules';

describe('password rules (spec 010 FR-004)', () => {
  it('accepts 8 to 128 characters with a letter and a digit that is not the e-mail', () => {
    expect(isPasswordValid(checkPassword('bep12345', 'du@example.com'))).toBe(true);
    expect(isPasswordValid(checkPassword('a1' + 'x'.repeat(126), 'du@example.com'))).toBe(true);
  });

  it.each([
    ['too short', 'bep1234', 'length'],
    ['too long', 'a1' + 'x'.repeat(127), 'length'],
    ['no letter', '12345678', 'letter'],
    ['no digit', 'bepnhatoi', 'digit'],
  ] as const)('refuses a password that is %s', (_label, password, rule) => {
    const check = checkPassword(password, 'du@example.com');

    expect(check[rule]).toBe(false);
    expect(isPasswordValid(check)).toBe(false);
  });

  it('refuses the e-mail as the password, ignoring case and spaces', () => {
    expect(checkPassword('Du1@Example.com', ' du1@example.com ').notEmail).toBe(false);
  });

  it('counts code points of the NFC form, not UTF-16 units (R3)', () => {
    // "😀" is two UTF-16 units but one character; a precomposed and a decomposed "ế" both count as one
    expect(passwordLength('😀')).toBe(1);
    expect(passwordLength('ế')).toBe(passwordLength('ế'));
    expect(checkPassword('😀'.repeat(4) + 'a1bc', 'x@y.vn').length).toBe(true); // 8 characters
  });

  it('accepts Vietnamese letters', () => {
    expect(checkPassword('Phở12345', 'x@y.vn').letter).toBe(true);
  });
});
