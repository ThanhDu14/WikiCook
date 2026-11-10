import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { AiGenerateDialog } from '../components/AiGenerateDialog';
import type { GenerateRequest } from '../types';
import { draftResponse, options, WEEK } from './fixtures';

function renderDialog(opts = options()) {
  const onDraft = vi.fn();
  const onClose = vi.fn();
  render(<AiGenerateDialog weekStart={WEEK} options={opts} onDraft={onDraft} onClose={onClose} />);
  return { onDraft, onClose };
}

const createButton = () => screen.getByRole('button', { name: 'Tạo thực đơn' });

describe('AiGenerateDialog (T033)', () => {
  it('shows the profile constraints filled in (US1-1)', () => {
    renderDialog();

    expect(screen.getByText('1 chất gây dị ứng')).toBeInTheDocument();
    expect(screen.getByText('2 nguyên liệu loại trừ')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Ăn chay' })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByRole('group', { name: 'Số người ăn' })).toHaveTextContent('4 người');
    expect(screen.getByRole('checkbox', { name: /Tối đa/ })).toBeChecked();
    expect(screen.getByRole('spinbutton', { name: 'Số phút tối đa' })).toHaveValue(45);
    expect(screen.getByRole('checkbox', { name: /Ưu tiên ẩm thực yêu thích/ })).toBeChecked();
    expect(screen.getByText('Còn 4/5 lượt tạo trong 60 phút.')).toBeInTheDocument();
  });

  it('selects every remaining day and all 3 meals by default, each can be unselected (US1-2)', async () => {
    renderDialog(options({ selectableDays: ['WEDNESDAY', 'THURSDAY'] }));
    const days = within(screen.getByRole('group', { name: 'Ngày' }));
    const meals = within(screen.getByRole('group', { name: 'Bữa' }));

    expect(days.getAllByRole('button').map((b) => b.textContent)).toEqual(['T4', 'T5']);
    days.getAllByRole('button').forEach((b) => expect(b).toHaveAttribute('aria-pressed', 'true'));
    meals.getAllByRole('button').forEach((b) => expect(b).toHaveAttribute('aria-pressed', 'true'));

    await userEvent.click(days.getByRole('button', { name: 'T4' }));
    expect(days.getByRole('button', { name: 'T4' })).toHaveAttribute('aria-pressed', 'false');
    expect(createButton()).toBeEnabled();
  });

  it('disables "Tạo" when no day or no meal is selected (edge case)', async () => {
    renderDialog(options({ selectableDays: ['MONDAY'] }));
    await userEvent.click(within(screen.getByRole('group', { name: 'Ngày' })).getByRole('button', { name: 'T2' }));
    expect(createButton()).toBeDisabled();

    await userEvent.click(within(screen.getByRole('group', { name: 'Ngày' })).getByRole('button', { name: 'T2' }));
    const meals = within(screen.getByRole('group', { name: 'Bữa' }));
    for (const name of ['Sáng', 'Trưa', 'Tối']) await userEvent.click(meals.getByRole('button', { name }));
    expect(createButton()).toBeDisabled();
  });

  it('offers "Theo công thức" for the servings', async () => {
    renderDialog();
    await userEvent.click(screen.getByRole('checkbox', { name: 'Theo công thức' }));

    expect(screen.getByRole('group', { name: 'Số người ăn' })).toHaveTextContent('Theo công thức');
  });

  it('starts from "Theo công thức" when the profile has no household size', () => {
    renderDialog(options({ defaults: { ...options().defaults, servings: null } }));

    expect(screen.getByRole('checkbox', { name: 'Theo công thức' })).toBeChecked();
  });

  it('says so when the profile is empty (FR-006)', () => {
    renderDialog(options({ profileIncomplete: true, defaults: { ...options().defaults, allergenIds: [], excludedIngredientIds: [], dietType: 'NONE' } }));

    expect(screen.getByText(/Hồ sơ ăn uống của bạn còn trống/)).toBeInTheDocument();
  });

  it('sends the chosen days, meals and constraints, then hands the draft over', async () => {
    let body: GenerateRequest | undefined;
    server.use(
      http.post(`/api/v1/ai-planner/weeks/${WEEK}/generations`, async ({ request }) => {
        body = (await request.json()) as GenerateRequest;
        return HttpResponse.json(draftResponse());
      }),
    );
    const { onDraft } = renderDialog(options({ selectableDays: ['MONDAY', 'TUESDAY'] }));
    await userEvent.click(within(screen.getByRole('group', { name: 'Bữa' })).getByRole('button', { name: 'Tối' }));
    await userEvent.click(screen.getByRole('checkbox', { name: /Ưu tiên ẩm thực yêu thích/ }));
    await userEvent.click(createButton());

    await vi.waitFor(() => expect(onDraft).toHaveBeenCalledWith(draftResponse(), body));
    expect(body).toMatchObject({
      days: ['MONDAY', 'TUESDAY'],
      meals: ['BREAKFAST', 'LUNCH'],
      constraints: { allergenIds: [3], excludedIngredientIds: [10, 11], dietType: 'VEGETARIAN', servings: 4, maxCookTimeMinutes: 45, preferredCuisineIds: [] },
    });
    expect(body?.requestId).toMatch(/^[0-9a-f-]{36}$/);
  });

  it('shows the progress while generating and goes back to the form on cancel (US1-4)', async () => {
    server.use(
      http.post(`/api/v1/ai-planner/weeks/${WEEK}/generations`, async () => {
        await new Promise((resolve) => setTimeout(resolve, 5_000));
        return HttpResponse.json(draftResponse());
      }),
    );
    const { onDraft } = renderDialog();
    await userEvent.click(createButton());

    expect(await screen.findByText('Đang chọn món cho tuần')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Hủy' }));

    expect(await screen.findByRole('button', { name: 'Tạo thực đơn' })).toBeInTheDocument();
    expect(onDraft).not.toHaveBeenCalled();
  });

  it('shows a Vietnamese message for an error code and stays on the form', async () => {
    server.use(
      http.post(`/api/v1/ai-planner/weeks/${WEEK}/generations`, () =>
        HttpResponse.json({ status: 409, code: 'GENERATION_IN_PROGRESS' }, { status: 409 }),
      ),
    );
    renderDialog();
    await userEvent.click(createButton());

    expect(await screen.findByRole('alert')).toHaveTextContent('Bạn đang có một lượt tạo khác chưa xong');
    expect(createButton()).toBeEnabled();
  });

  it('renders outside the button it was opened from, so the empty week styles do not leak in', () => {
    // the empty week of 030 holds "Tạo bằng AI" inside `wk-state` (centred text, `p` at most 36ch)
    render(
      <section className="wk-state" data-testid="empty-week">
        <AiGenerateDialog weekStart={WEEK} options={options()} onDraft={vi.fn()} onClose={vi.fn()} />
      </section>,
    );

    const dialog = screen.getAllByRole('dialog', { name: 'Tạo thực đơn bằng AI' }).at(-1)!;
    expect(screen.getByTestId('empty-week')).not.toContainElement(dialog);
  });

  it('closes with "Hủy" or the close button', async () => {
    const { onClose } = renderDialog();
    await userEvent.click(screen.getByRole('button', { name: 'Đóng' }));
    expect(onClose).toHaveBeenCalled();
  });
});
