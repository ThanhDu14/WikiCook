/** Password rules of spec 010 FR-004, the same as the backend (`PasswordPolicy`): the server stays the judge. */

export interface PasswordCheck {
  length: boolean;
  letter: boolean;
  digit: boolean;
  notEmail: boolean;
}

/** Length counts code points of the NFC form, as the backend does (research R3), not UTF-16 units. */
export function passwordLength(value: string): number {
  return [...value.normalize('NFC')].length;
}

export function checkPassword(password: string, email: string): PasswordCheck {
  const length = passwordLength(password);
  const normalizedEmail = email.trim().toLowerCase();
  return {
    length: length >= 8 && length <= 128,
    letter: /\p{L}/u.test(password),
    digit: /\p{Nd}/u.test(password),
    notEmail: normalizedEmail === '' || password.trim().toLowerCase() !== normalizedEmail,
  };
}

export const isPasswordValid = (check: PasswordCheck) => check.length && check.letter && check.digit && check.notEmail;
