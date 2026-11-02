import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { RecipePicker } from '../components/RecipePicker';
import type { PickerResult } from '../types';

const result = (overrides: Partial<PickerResult> = {}): PickerResult => ({
  recipeId: 1,
  title: 'Trứng chiên',
  coverImageUrl: null,
  totalTimeMinutes: 10,
  cuisineName: 'Việt Nam',
  mealSuited: true,
  conflicts: [],
  ...overrides,
});

function pickerApi(pages: (url: URL) => PickerResult[]) {
  const calls: URL[] = [];
  server.use(
    http.get('/api/v1/meal-plans/picker/recipes', ({ request }) => {
      const url = new URL(request.url);
      calls.push(url);
      return HttpResponse.json({ content: pages(url), page: 0, size: 20, hasNext: false });
    }),
  );
  return calls;
}

function renderPicker(props: Partial<Parameters<typeof RecipePicker>[0]> = {}) {
  const onPick = vi.fn().mockResolvedValue(null);
  const onClose = vi.fn();
  render(<RecipePicker day="WEDNESDAY" meal="DINNER" onPick={onPick} onClose={onClose} {...props} />);
  return { onPick, onClose };
}

describe('RecipePicker (T036)', () => {
  it('opens for the meal of the slot (US1-2)', async () => {
    const calls = pickerApi(() => [result()]);
    renderPicker();

    expect(screen.getByRole('dialog', { name: 'Chọn món · Tối T4' })).toBeInTheDocument();
    expect(await screen.findByText('Trứng chiên')).toBeInTheDocument();
    expect(calls[0].searchParams.get('meal')).toBe('DINNER');
    expect(calls[0].searchParams.get('scope')).toBe('ALL');
  });

  it('searches once the user stops typing', async () => {
    const calls = pickerApi((url) => (url.searchParams.get('q') === 'pho' ? [result({ title: 'Phở bò' })] : []));
    renderPicker();
    await waitFor(() => expect(calls).toHaveLength(1));

    await userEvent.type(screen.getByRole('searchbox', { name: 'Tìm kiếm' }), 'pho');

    expect(await screen.findByText('Phở bò')).toBeInTheDocument();
    expect(calls.map((c) => c.searchParams.get('q'))).toEqual([null, 'pho']);
  });

  it('switches to my collections', async () => {
    const calls = pickerApi(() => []);
    renderPicker();

    await userEvent.click(screen.getByRole('button', { name: 'Bộ sưu tập' }));

    await waitFor(() => expect(calls.at(-1)?.searchParams.get('scope')).toBe('COLLECTIONS'));
    expect(screen.getByRole('button', { name: 'Bộ sưu tập' })).toHaveAttribute('aria-pressed', 'true');
    expect(await screen.findByText('Bộ sưu tập của bạn chưa có công thức nào.')).toBeInTheDocument();
  });

  it('renders the suggestions slot above the results only when given (FR-007c)', async () => {
    pickerApi(() => [result()]);
    const { unmount } = render(
      <RecipePicker day="MONDAY" meal="LUNCH" onPick={vi.fn()} onClose={vi.fn()} suggestionsSlot={<p>Gợi ý của AI</p>} />,
    );
    const suggestions = await screen.findByText('Gợi ý của AI');
    const firstResult = await screen.findByText('Trứng chiên');
    expect(suggestions.compareDocumentPosition(firstResult) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy();
    unmount();

    renderPicker();
    await screen.findByText('Trứng chiên');
    expect(screen.queryByTestId('picker-suggestions')).not.toBeInTheDocument();
  });

  it('warns about conflicts and asks before adding (US1-4)', async () => {
    pickerApi(() => [result({ recipeId: 3, title: 'Xôi đậu phộng', conflicts: [{ type: 'ALLERGEN', label: 'Đậu phộng' }] })]);
    const { onPick } = renderPicker();

    const card = (await screen.findByText('Xôi đậu phộng')).closest('li')!;
    expect(within(card).getByText('Có Đậu phộng – bạn bị dị ứng')).toBeInTheDocument();

    await userEvent.click(within(card).getByRole('button', { name: 'Chọn' }));
    const confirm = screen.getByRole('alertdialog', { name: 'Món này không hợp hồ sơ của bạn' });
    await userEvent.click(within(confirm).getByRole('button', { name: 'Hủy' }));
    expect(onPick).not.toHaveBeenCalled();

    await userEvent.click(within(card).getByRole('button', { name: 'Chọn' }));
    await userEvent.click(screen.getByRole('button', { name: 'Vẫn thêm' }));
    expect(onPick).toHaveBeenCalledWith(expect.objectContaining({ recipeId: 3 }));
  });

  it('picks a dish without conflicts directly', async () => {
    pickerApi(() => [result()]);
    const { onPick } = renderPicker();

    await userEvent.click(await screen.findByRole('button', { name: 'Chọn' }));

    expect(onPick).toHaveBeenCalledWith(expect.objectContaining({ recipeId: 1, title: 'Trứng chiên' }));
  });

  it('suggests another name when nothing is found (US1-10)', async () => {
    pickerApi(() => []);
    renderPicker();

    expect(await screen.findByText('Không tìm thấy công thức phù hợp.')).toBeInTheDocument();
  });

  it('reloads the list when the recipe was archived meanwhile (422)', async () => {
    let calls = 0;
    pickerApi(() => (calls++ === 0 ? [result({ title: 'Bò lúc lắc' })] : [result({ recipeId: 2, title: 'Gà nướng' })]));
    const onPick = vi.fn().mockResolvedValue('RECIPE_NOT_AVAILABLE');
    renderPicker({ onPick });

    await userEvent.click(await screen.findByRole('button', { name: 'Chọn' }));

    expect(await screen.findByText('Công thức này không còn khả dụng.')).toBeInTheDocument();
    expect(await screen.findByText('Gà nướng')).toBeInTheDocument();
  });

  it('closes with the back button', async () => {
    pickerApi(() => []);
    const { onClose } = renderPicker();

    await userEvent.click(screen.getByRole('button', { name: 'Quay lại' }));

    expect(onClose).toHaveBeenCalled();
  });
});
