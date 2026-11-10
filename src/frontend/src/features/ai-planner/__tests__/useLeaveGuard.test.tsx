import { act, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createMemoryRouter, Link, RouterProvider, useSearchParams } from 'react-router-dom';
import { LeaveDraftDialog } from '../components/LeaveDraftDialog';
import { clearAllAiDrafts, useAiDraft } from '../hooks/useAiDraft';
import { useLeaveGuard } from '../hooks/useLeaveGuard';
import { draftResponse, WEEK } from './fixtures';

afterEach(() => clearAllAiDrafts());

/** The planner page as far as the guard sees it: the week of `?week=`, links to another page and week. */
function Planner() {
  const [params, setParams] = useSearchParams();
  const weekStart = params.get('week') ?? WEEK;
  const { draft, setDraft } = useAiDraft(weekStart);
  const guard = useLeaveGuard(weekStart);
  return (
    <>
      <p>{draft ? 'có nháp' : 'không nháp'}</p>
      <button type="button" onClick={() => setDraft(draftResponse({ weekStart }))}>
        tạo
      </button>
      <button type="button" onClick={() => setParams({ week: '2026-11-23' })}>
        tuần sau
      </button>
      <Link to="/recipes">công thức</Link>
      <Link to="/login?redirect=%2Fmeal-planner">đăng nhập</Link>
      <LeaveDraftDialog guard={guard} count={draft?.items.length ?? 0} />
    </>
  );
}

function renderPlanner() {
  const router = createMemoryRouter(
    [
      { path: '/meal-planner', element: <Planner /> },
      { path: '/recipes', element: <p>trang công thức</p> },
      { path: '/login', element: <p>trang đăng nhập</p> },
    ],
    { initialEntries: [`/meal-planner?week=${WEEK}`] },
  );
  render(<RouterProvider router={router} />);
  return router;
}

describe('useLeaveGuard (T050, US2-4, FR-022)', () => {
  it('asks before closing the tab only while there is a draft', async () => {
    renderPlanner();
    const before = () => {
      const event = new Event('beforeunload', { cancelable: true });
      window.dispatchEvent(event);
      return event.defaultPrevented;
    };

    expect(before()).toBe(false);
    await userEvent.click(screen.getByRole('button', { name: 'tạo' }));
    expect(before()).toBe(true);
  });

  it('asks before going to another page; "Ở lại" keeps the draft', async () => {
    const router = renderPlanner();
    await userEvent.click(screen.getByRole('button', { name: 'tạo' }));
    await userEvent.click(screen.getByRole('link', { name: 'công thức' }));

    const dialog = await screen.findByRole('dialog', { name: 'Bỏ bản nháp?' });
    expect(dialog).toHaveTextContent('2 món AI gợi ý chưa được lưu vào thực đơn. Nếu rời trang, bản nháp sẽ mất.');
    await userEvent.click(screen.getByRole('button', { name: 'Ở lại' }));

    expect(router.state.location.pathname).toBe('/meal-planner');
    expect(screen.getByText('có nháp')).toBeInTheDocument();
  });

  it('asks before changing week; "Bỏ nháp" drops the draft and goes on', async () => {
    const router = renderPlanner();
    await userEvent.click(screen.getByRole('button', { name: 'tạo' }));
    await userEvent.click(screen.getByRole('button', { name: 'tuần sau' }));
    await userEvent.click(await screen.findByRole('button', { name: 'Bỏ nháp' }));

    expect(router.state.location.search).toBe('?week=2026-11-23');
    expect(screen.getByText('không nháp')).toBeInTheDocument();
    // the draft of the first week is gone too
    await act(() => router.navigate(`/meal-planner?week=${WEEK}`));
    expect(screen.getByText('không nháp')).toBeInTheDocument();
  });

  it('does not ask without a draft', async () => {
    const router = renderPlanner();
    await userEvent.click(screen.getByRole('link', { name: 'công thức' }));

    expect(router.state.location.pathname).toBe('/recipes');
  });

  it('an expired session drops the draft and goes to sign-in without asking (edge case)', async () => {
    const router = renderPlanner();
    await userEvent.click(screen.getByRole('button', { name: 'tạo' }));
    await userEvent.click(screen.getByRole('link', { name: 'đăng nhập' }));

    expect(router.state.location.pathname).toBe('/login');
    expect(router.state.location.search).toBe('?redirect=%2Fmeal-planner');
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
    await act(() => router.navigate(`/meal-planner?week=${WEEK}`));
    expect(screen.getByText('không nháp')).toBeInTheDocument();
  });

  it('losing the connection drops the draft (edge case)', async () => {
    renderPlanner();
    await userEvent.click(screen.getByRole('button', { name: 'tạo' }));
    act(() => {
      window.dispatchEvent(new Event('offline'));
    });

    expect(screen.getByText('không nháp')).toBeInTheDocument();
  });
});
