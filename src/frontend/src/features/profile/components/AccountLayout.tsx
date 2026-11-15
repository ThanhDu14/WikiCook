import type { ReactNode } from 'react';
import { NavLink } from 'react-router-dom';
import { useSignOut } from '@/features/auth/hooks/useSignOut';
import { initials } from '@/features/auth/lib/initials';
import { useAuth } from '@/features/auth/useAuth';
import { Icon, type IconName } from '@/shared/ui/Icon';
import { strings } from '../strings.vi';

const SOON = 'Sắp có';

/** Menu entries of W-PF01; those without `to` belong to pages that are not built yet, shown but not links. */
const ITEMS: ReadonlyArray<{ label: string; icon: IconName; to?: string; end?: boolean }> = [
  { label: strings.nav.overview, icon: 'user', to: '/profile', end: true },
  { label: strings.nav.publicProfile, icon: 'eye', to: '/profile/edit' },
  { label: strings.nav.dietary, icon: 'salad', to: '/profile/dietary' },
  { label: strings.nav.myRecipes, icon: 'chef' },
  { label: strings.nav.shoppingHistory, icon: 'history', to: '/shopping-list/history' },
  { label: strings.nav.security, icon: 'lock' },
];

/** The account pages of the screens (PF01, PF03, PF04): a side menu on the left, the page on the right. */
export function AccountLayout({ children }: { children: ReactNode }) {
  const { account } = useAuth();
  const signOut = useSignOut();
  return (
    <div className="wx-split wx-split--left wk-desktop">
      <div className="wx-aside">
        <nav className="wx-side" aria-label={strings.nav.label}>
          {account ? (
            <div className="sx-row" style={{ gap: 12, padding: '0 12px 16px' }}>
              <span className="wk-avatar" aria-hidden="true">
                {initials(account)}
              </span>
              <div>
                <strong>{account.displayName}</strong>
                <p className="wk-caption wk-muted">{account.email}</p>
              </div>
            </div>
          ) : null}
          {ITEMS.map(({ label, icon, to, end }) =>
            to ? (
              <NavLink key={label} className="wk-navlink" to={to} end={end}>
                <Icon name={icon} />
                {label}
              </NavLink>
            ) : (
              <span key={label} className="wk-navlink cursor-default" aria-disabled="true" title={SOON}>
                <Icon name={icon} />
                {label}
              </span>
            ),
          )}
          <hr className="wk-rule" style={{ margin: '8px 0' }} />
          <button type="button" className="wk-navlink" onClick={signOut} style={{ background: 'none', border: 0, textAlign: 'left', width: '100%' }}>
            <Icon name="logout" />
            {strings.nav.signOut}
          </button>
          <span className="wk-navlink cursor-default" aria-disabled="true" title={SOON} style={{ color: 'var(--color-danger)' }}>
            <Icon name="trash" />
            {strings.nav.deleteAccount}
          </span>
        </nav>
      </div>
      <div className="sx-stack" style={{ gap: 24, minWidth: 0 }}>
        {children}
      </div>
    </div>
  );
}
