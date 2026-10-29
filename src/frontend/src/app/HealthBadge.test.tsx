import { render, screen } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { HealthBadge } from './HealthBadge';

describe('HealthBadge', () => {
  it('shows UP when the backend health endpoint reports UP', async () => {
    render(<HealthBadge />);

    expect(screen.getByRole('status')).toHaveTextContent('Backend: đang kiểm tra');
    expect(await screen.findByText('Backend: hoạt động')).toBeInTheDocument();
  });

  it('shows DOWN when the backend reports DOWN', async () => {
    server.use(http.get('/actuator/health', () => HttpResponse.json({ status: 'DOWN' }, { status: 503 })));
    render(<HealthBadge />);

    expect(await screen.findByText('Backend: mất kết nối')).toBeInTheDocument();
  });

  it('shows DOWN when the backend cannot be reached', async () => {
    server.use(http.get('/actuator/health', () => HttpResponse.error()));
    render(<HealthBadge />);

    expect(await screen.findByText('Backend: mất kết nối')).toBeInTheDocument();
  });
});
