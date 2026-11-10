import type { ReactNode } from 'react';
import { NavLink, Outlet } from 'react-router-dom';
import { HealthBadge } from './HealthBadge';
import { BagIcon, BellIcon, BookmarkIcon, CalendarIcon, CompassIcon, PenIcon, SearchIcon, UserIcon } from './navIcons';

type Icon = (props: { size?: 24 }) => ReactNode;

/** The main destinations of the screens; those without `to` belong to modules that are not built yet. */
const MAIN: { label: string; icon: Icon; to?: string }[] = [
  { label: 'Khám phá', icon: CompassIcon },
  { label: 'Thực đơn', icon: CalendarIcon, to: '/meal-planner' },
  { label: 'Đi chợ', icon: BagIcon, to: '/shopping-list' },
  { label: 'Đã lưu', icon: BookmarkIcon },
];

const MOBILE: typeof MAIN = [...MAIN, { label: 'Tôi', icon: UserIcon }];

const SOON = 'Sắp có';

/** `.wk-bottomnav a` of the design, for the tabs that are not links yet. */
const SOON_TAB = {
  display: 'grid',
  justifyItems: 'center',
  gap: 2,
  minHeight: 56,
  paddingTop: 'var(--space-2)',
  color: 'var(--color-ink-muted)',
  font: 'var(--text-caption)',
  cursor: 'default',
} as const;

function Logo({ small }: { small?: boolean }) {
  return (
    <NavLink to="/" className="wk-logo" aria-label="WikiCook" style={small ? { fontSize: 20 } : undefined}>
      Wiki<span>Cook</span>
    </NavLink>
  );
}

/** Desktop header of W-MP01: logo, main links, search, "Viết công thức", notifications and account. */
function Header() {
  return (
    <header className="wx-nav">
      <div className="wx-wrap wx-nav__in">
        <Logo />
        <nav className="wx-nav__links" aria-label="Điều hướng chính">
          {MAIN.map(({ label, icon: ItemIcon, to }) =>
            to ? (
              <NavLink key={label} to={to} className="wk-navlink">
                <ItemIcon />
                {label}
              </NavLink>
            ) : (
              <span key={label} className="wk-navlink cursor-default" aria-disabled="true" title={SOON}>
                <ItemIcon />
                {label}
              </span>
            ),
          )}
        </nav>
        <div className="wk-search wx-nav__search max-[900px]:!hidden">
          <SearchIcon />
          <input className="wk-input" placeholder="Tìm món, nguyên liệu…" aria-label="Tìm kiếm" title={SOON} disabled />
        </div>
        <div className="wx-nav__end ml-auto min-[901px]:ml-0">
          <span className="wk-btn wk-btn--secondary wk-btn--sm cursor-default max-[900px]:!hidden" aria-disabled="true" title={SOON}>
            <PenIcon />
            Viết công thức
          </span>
          <button className="wk-icon-btn wk-icon-btn--plain" type="button" aria-label="Thông báo" title={SOON} disabled>
            <BellIcon />
          </button>
          <span className="wk-navlink" style={{ padding: '0 4px' }} aria-label="Tài khoản của tôi" title={SOON}>
            <span className="wk-avatar wk-avatar--sm">
              <UserIcon />
            </span>
          </span>
        </div>
      </div>
    </header>
  );
}

/** Footer of the desktop screens (W-MP01); the backend status stays visible for the FE ↔ BE wiring. */
function Footer() {
  return (
    <footer className="wx-foot max-[900px]:hidden">
      <div className="wx-wrap wx-foot__in">
        <Logo small />
        <span>Công thức nấu ăn do cộng đồng biên soạn</span>
        <nav aria-label="Liên kết chân trang">
          {['Giới thiệu', 'Quy tắc cộng đồng', 'Điều khoản', 'Quyền riêng tư'].map((label) => (
            <span key={label} title={SOON}>
              {label}
            </span>
          ))}
        </nav>
        <HealthBadge />
      </div>
    </footer>
  );
}

/** Bottom navigation of the mobile screens (MP01), under 900px where the header links are hidden. */
function BottomNav() {
  return (
    <nav className="wk-bottomnav fixed inset-x-0 bottom-0 z-40 min-[901px]:!hidden" aria-label="Điều hướng chính">
      {MOBILE.map(({ label, icon: ItemIcon, to }) =>
        to ? (
          <NavLink key={label} to={to}>
            <ItemIcon size={24} />
            {label}
          </NavLink>
        ) : (
          <span key={label} style={SOON_TAB} aria-disabled="true" title={SOON}>
            <ItemIcon size={24} />
            {label}
          </span>
        ),
      )}
    </nav>
  );
}

export function AppLayout() {
  return (
    <div className="wx-page min-h-screen">
      <Header />
      {/* under 900px the bottom navigation covers the last 64px */}
      <main className="flex-1 pb-[88px] pt-ds-6 min-[901px]:pb-ds-6">
        {/* every page in the 1264px column of the screens (W-MP01 too) */}
        <div className="wx-wrap">
          <Outlet />
        </div>
      </main>
      <Footer />
      <BottomNav />
    </div>
  );
}
