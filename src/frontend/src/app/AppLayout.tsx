import { NavLink, Outlet, useMatches } from 'react-router-dom';
import { HealthBadge } from './HealthBadge';

/** Routes with `handle: { wide: true }` (the week planner) use the screen width instead of the reading column. */
function useWide() {
  return useMatches().some((match) => (match.handle as { wide?: boolean } | undefined)?.wide);
}

export function AppLayout() {
  const wide = useWide();
  return (
    <div className="min-h-screen bg-bg text-ink">
      <header className="border-b border-border bg-surface">
        <div className="mx-auto flex max-w-content items-center justify-between gap-ds-4 px-ds-4 py-ds-3">
          <NavLink to="/" className="font-display text-2xl font-semibold text-primary">
            WikiCook
          </NavLink>
          <nav aria-label="Chính" className="flex items-center gap-ds-4">
            <NavLink
              to="/meal-planner"
              className={({ isActive }) => (isActive ? 'font-semibold text-primary' : 'text-ink-muted')}
            >
              Thực đơn
            </NavLink>
            <NavLink
              to="/shopping-list"
              className={({ isActive }) => (isActive ? 'font-semibold text-primary' : 'text-ink-muted')}
            >
              Danh sách đi chợ
            </NavLink>
            <HealthBadge />
          </nav>
        </div>
      </header>
      <main className={wide ? 'mx-auto w-full max-w-[1680px] px-ds-6 py-ds-6' : 'mx-auto max-w-content px-ds-4 py-ds-6'}>
        <Outlet />
      </main>
    </div>
  );
}
