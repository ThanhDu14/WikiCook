import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { AiGenerateDialog } from '../components/AiGenerateDialog';
import { NotEnoughRecipesMessage } from '../components/NotEnoughRecipesMessage';
import type { GenerateRequest } from '../types';
import { options, WEEK } from './fixtures';

const notEnough = {
  type: 'about:blank',
  status: 422,
  code: 'NOT_ENOUGH_RECIPES',
  eligibleCount: 6,
  requestedSlots: 14,
  suggestions: ['INCREASE_MAX_COOK_TIME', 'REMOVE_PREFERRED_CUISINES', 'SELECT_FEWER_SLOTS'],
};

describe('NotEnoughRecipesMessage (T065, US4-1, US4-2, W-AI06)', () => {
  it('says how many recipes were found for how many slots and lists every suggestion in Vietnamese', async () => {
    const onBack = vi.fn();
    render(<NotEnoughRecipesMessage eligibleCount={6} requestedSlots={14} suggestions={['INCREASE_MAX_COOK_TIME', 'SELECT_FEWER_SLOTS']} onBack={onBack} />);

    expect(screen.getByRole('heading', { name: 'Chỉ tìm được 6 món phù hợp' })).toBeInTheDocument();
    expect(screen.getByText('Với các điều kiện đã chọn, WikiCook chưa đủ món cho 14 ô. AI không lặp món để lấp chỗ trống.')).toBeInTheDocument();
    const list = screen.getByRole('list', { name: 'Gợi ý' });
    expect(within(list).getAllByRole('listitem').map((li) => li.textContent)).toEqual([
      'Tăng thời gian nấu tối đa',
      'Chọn ít ngày hoặc ít bữa hơn',
    ]);

    await userEvent.click(screen.getByRole('button', { name: 'Nới điều kiện' }));
    expect(onBack).toHaveBeenCalledOnce();
  });

  it('a 422 in the dialog shows the message, then "Nới điều kiện" goes back with the same choices', async () => {
    const sent: GenerateRequest[] = [];
    server.use(
      http.post(`/api/v1/ai-planner/weeks/${WEEK}/generations`, async ({ request }) => {
        sent.push((await request.json()) as GenerateRequest);
        return HttpResponse.json(notEnough, { status: 422 });
      }),
    );
    const onDraft = vi.fn();
    render(<AiGenerateDialog weekStart={WEEK} options={options()} onDraft={onDraft} onClose={vi.fn()} />);
    await userEvent.click(within(screen.getByRole('group', { name: 'Ngày' })).getByRole('button', { name: 'T7' }));
    await userEvent.click(screen.getByRole('button', { name: 'Thuần chay' }));
    await userEvent.click(screen.getByRole('button', { name: 'Tạo thực đơn' }));

    expect(await screen.findByRole('heading', { name: 'Chỉ tìm được 6 món phù hợp' })).toBeInTheDocument();
    expect(screen.getAllByRole('listitem').map((li) => li.textContent)).toEqual([
      'Tăng thời gian nấu tối đa',
      'Bỏ ưu tiên ẩm thực',
      'Chọn ít ngày hoặc ít bữa hơn',
    ]);
    expect(screen.queryByRole('button', { name: 'Tạo thực đơn' })).not.toBeInTheDocument();
    expect(onDraft).not.toHaveBeenCalled();

    await userEvent.click(screen.getByRole('button', { name: 'Nới điều kiện' }));

    expect(within(screen.getByRole('group', { name: 'Ngày' })).getByRole('button', { name: 'T7' })).toHaveAttribute('aria-pressed', 'false');
    expect(screen.getByRole('button', { name: 'Thuần chay' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('spinbutton', { name: 'Số phút tối đa' })).toHaveValue(45);
    expect(screen.getByRole('button', { name: 'Tạo thực đơn' })).toBeEnabled();
    expect(sent[0].days).not.toContain('SATURDAY');
  });

  it('a 422 without details still explains and goes back', async () => {
    server.use(
      http.post(`/api/v1/ai-planner/weeks/${WEEK}/generations`, () =>
        HttpResponse.json({ status: 422, code: 'NOT_ENOUGH_RECIPES' }, { status: 422 }),
      ),
    );
    render(<AiGenerateDialog weekStart={WEEK} options={options()} onDraft={vi.fn()} onClose={vi.fn()} />);
    await userEvent.click(screen.getByRole('button', { name: 'Tạo thực đơn' }));

    expect(await screen.findByRole('heading', { name: 'Chỉ tìm được 0 món phù hợp' })).toBeInTheDocument();
    expect(screen.getAllByRole('listitem').map((li) => li.textContent)).toEqual(['Chọn ít ngày hoặc ít bữa hơn']);
  });
});
