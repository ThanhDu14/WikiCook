import { Link } from 'react-router-dom';
import { initials } from '../lib/initials';
import { strings } from '../strings.vi';
import { useAuth } from '../useAuth';

/**
 * The account corner of the header (W-MP01 …): the avatar with the user's initials, a link to `/profile`; "Đăng nhập"
 * for a guest. Sign-out is in the account menu (W-PF01), as in the screens, not in the header.
 */
export function AccountMenu() {
  const { status, account } = useAuth();

  if (status === 'loading') {
    return <span className="wk-avatar wk-avatar--sm" aria-hidden="true" />;
  }
  if (!account) {
    return (
      <Link className="wk-btn wk-btn--primary wk-btn--sm" to="/login">
        {strings.nav.signIn}
      </Link>
    );
  }
  return (
    <Link className="wk-navlink" to="/profile" aria-label={strings.nav.account} title={account.displayName} style={{ padding: '0 4px' }}>
      <span className="wk-avatar wk-avatar--sm">{initials(account)}</span>
    </Link>
  );
}
