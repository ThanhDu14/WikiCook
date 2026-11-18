import { useState } from 'react';
import { initials } from '../lib/initials';
import type { CurrentAccount } from '../types';

/**
 * The avatar of the signed-in user: the picture when the account has one, the initials otherwise (also when the
 * picture cannot be loaded). Decorative: the link or text around it already names the account.
 */
export function AccountAvatar({ account, size }: { account: Pick<CurrentAccount, 'displayName' | 'avatarUrl'>; size?: 'sm' | 'lg' | 'xl' }) {
  const [failed, setFailed] = useState<string | null>(null);
  const className = size ? `wk-avatar wk-avatar--${size}` : 'wk-avatar';
  if (account.avatarUrl && failed !== account.avatarUrl) {
    return (
      <img className={className} src={account.avatarUrl} alt="" style={{ objectFit: 'cover' }} onError={() => setFailed(account.avatarUrl ?? null)} />
    );
  }
  return (
    <span className={className} aria-hidden="true">
      {initials(account)}
    </span>
  );
}
