import { useEffect, useState } from 'react';
import { fetchJson } from '@/shared/api/client';

type Health = 'checking' | 'up' | 'down';

const LABELS: Record<Health, string> = {
  checking: 'Backend: đang kiểm tra',
  up: 'Backend: hoạt động',
  down: 'Backend: mất kết nối',
};

const DOT: Record<Health, string> = {
  checking: 'bg-border-strong',
  up: 'bg-success',
  down: 'bg-danger',
};

const POLL_MS = 30_000;

/** Shows whether the Spring Boot backend answers /actuator/health, so the FE ↔ BE wiring is visible at a glance. */
export function HealthBadge() {
  const [health, setHealth] = useState<Health>('checking');

  useEffect(() => {
    let active = true;
    const check = () =>
      fetchJson<{ status: string }>('/actuator/health')
        .then((body) => body.status === 'UP')
        .catch(() => false)
        .then((up) => {
          if (active) setHealth(up ? 'up' : 'down');
        });

    check();
    const timer = window.setInterval(check, POLL_MS);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, []);

  return (
    <span
      role="status"
      className="inline-flex items-center gap-ds-2 rounded-pill bg-surface-alt px-ds-3 py-ds-1 text-sm text-ink-muted"
    >
      <span aria-hidden="true" className={`h-2 w-2 rounded-pill ${DOT[health]}`} />
      {LABELS[health]}
    </span>
  );
}
