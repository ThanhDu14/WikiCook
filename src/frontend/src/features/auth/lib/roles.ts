import { ROLE_ORDER, type CurrentAccount, type Role } from '../types';

/** `true` when the account's role is `minRole` or stronger (FR-021); for hiding buttons, the server checks again. */
export function hasRole(account: CurrentAccount | null, minRole: Role): boolean {
  return account !== null && ROLE_ORDER.indexOf(account.role) >= ROLE_ORDER.indexOf(minRole);
}
