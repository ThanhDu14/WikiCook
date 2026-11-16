import type { CurrentAccount } from '../types';

/** Two letters for the avatar: the first letters of the first and last word of the display name. */
export function initials(account: Pick<CurrentAccount, 'displayName'>): string {
  const words = account.displayName.trim().split(/\s+/).filter(Boolean);
  if (words.length === 0) return '?';
  const first = [...words[0]][0] ?? '';
  const last = words.length > 1 ? ([...words[words.length - 1]][0] ?? '') : '';
  return (first + last).toLocaleUpperCase('vi-VN');
}
