import { NavLink, Outlet } from 'react-router-dom';
import { HealthBadge } from './HealthBadge';

export function AppLayout() {
  return (
    <div className="min-h-screen bg-bg text-ink">
      <header className="border-b border-border bg-surface">
        <div className="mx-auto flex max-w-content items-center justify-between gap-ds-4 px-ds-4 py-ds-3">
          <NavLink to="/" className="font-display text-2xl font-semibold text-primary">
            WikiCook
          </NavLink>
          <nav aria-label="Chính" className="flex items-center gap-ds-4">
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
      <main className="mx-auto max-w-content px-ds-4 py-ds-6">
        <Outlet />
      </main>
    </div>
  );
}
