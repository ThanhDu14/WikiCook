import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { http, HttpResponse } from 'msw';
import { MemoryRouter } from 'react-router-dom';
import { server } from '@/test/server';
import { makeGroup, makeItem, makeList } from '../test/fixtures';
import type { ShoppingList } from '../services/types';
import { ChecklistScreen } from './ChecklistScreen';

const BASE = '/api/v1/shopping-lists';

const beef = makeItem({
  clientUuid: 'beef',
  ingredientId: 1,
  displayName: 'Thịt bò',
  displayQuantity: 1,
  displayUnit: 'kg',
  aisleCategoryId: 2,
  sourceRecipeCount: 2,
  sources: [
    { sourceId: 1, recipeId: 1, mealPlanItemId: null, recipeTitleSnapshot: 'Bò kho', contributedQuantity: 600, unitName: 'g' },
    { sourceId: 2, recipeId: 2, mealPlanItemId: null, recipeTitleSnapshot: 'Bò xào', contributedQuantity: 400, unitName: 'g' },
  ],
});
const fishSauce = makeItem({ clientUuid: 'nm', displayName: 'Nước mắm', isChecked: true });

const fullList = makeList({
  totalItems: 2,
  checkedItems: 1,
  aisleGroups: [
    makeGroup([beef], { aisleId: 2, aisleCode: 'MEAT_SEAFOOD', aisleName: 'Thịt & Thủy hải sản', displayOrder: 2 }),
    makeGroup([fishSauce]),
  ],
});

function setup(list: ShoppingList = fullList) {
  server.use(http.get(`${BASE}/active`, () => HttpResponse.json(list)));
  render(
    <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <ChecklistScreen />
    </MemoryRouter>,
  );
  return userEvent.setup();
}

describe('ChecklistScreen (SL-01 / SL-02)', () => {
  it('shows the empty state of SL-02 for a fresh list', async () => {
    setup(makeList());

    expect(await screen.findByRole('heading', { level: 1, name: 'Danh sách mua sắm 31/10' })).toBeInTheDocument();
    expect(screen.getByText('Danh sách đi chợ')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Danh sách đang trống' })).toBeInTheDocument();
    expect(screen.getByText('Đã mua 0/0 món')).toBeInTheDocument();
  });

  it('groups items by aisle with source lines and quantities', async () => {
    setup();

    const meat = (await screen.findByRole('heading', { name: 'Thịt & Thủy hải sản' })).closest('section')!;
    expect(within(meat).getByText('Thịt bò')).toBeInTheDocument();
    expect(within(meat).getByText('600 g Bò kho · 400 g Bò xào')).toBeInTheDocument();
    expect(within(meat).getByText('1 kg')).toBeInTheDocument();
    expect(within(meat).getByText('còn 1')).toBeInTheDocument();

    const general = screen.getByRole('heading', { name: 'Đồ dùng khác' }).closest('section')!;
    expect(within(general).getByText('còn 0')).toBeInTheDocument();
    expect(screen.getByText('Đã mua 1/2 món')).toBeInTheDocument();
    expect(screen.getByText('50%')).toBeInTheDocument();
  });

  it('adds a custom item into the General aisle', async () => {
    const user = setup(makeList());
    server.use(
      http.post(`${BASE}/active/items`, () =>
        HttpResponse.json(makeList({ aisleGroups: [makeGroup([makeItem({ displayName: 'Nước mắm' })])] }), { status: 201 }),
      ),
    );

    await user.click((await screen.findAllByRole('button', { name: 'Thêm món' }))[0]);
    await user.type(screen.getByLabelText('Tên món'), 'Nước mắm');
    await user.click(screen.getByRole('button', { name: 'Thêm vào danh sách' }));

    const general = (await screen.findByRole('heading', { name: 'Đồ dùng khác' })).closest('section')!;
    expect(within(general).getByText('Nước mắm')).toBeInTheDocument();
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('renames the list inline and saves on Enter', async () => {
    let saved = '';
    server.use(
      http.patch(`${BASE}/active`, async ({ request }) => {
        saved = ((await request.json()) as { title: string }).title;
        return HttpResponse.json(makeList({ ...fullList, title: saved }));
      }),
    );
    const user = setup();

    await user.click(await screen.findByRole('button', { name: /Đổi tên danh sách/ }));
    const input = screen.getByRole('textbox', { name: 'Tên danh sách' });
    expect(input).toHaveAttribute('maxLength', '60');
    await user.clear(input);
    await user.type(input, 'Tiệc cuối tuần{Enter}');

    expect(await screen.findByRole('heading', { level: 1, name: 'Tiệc cuối tuần' })).toBeInTheDocument();
    expect(saved).toBe('Tiệc cuối tuần');
  });

  it('saves the title on blur and ignores an empty title', async () => {
    let calls = 0;
    server.use(
      http.patch(`${BASE}/active`, () => {
        calls += 1;
        return HttpResponse.json(fullList);
      }),
    );
    const user = setup();

    await user.click(await screen.findByRole('button', { name: /Đổi tên danh sách/ }));
    await user.clear(screen.getByRole('textbox', { name: 'Tên danh sách' }));
    await user.tab();

    expect(screen.getByRole('heading', { level: 1, name: 'Danh sách mua sắm 31/10' })).toBeInTheDocument();
    expect(calls).toBe(0);
  });

  it('archives the list after confirmation and shows the fresh list', async () => {
    server.use(
      http.post(`${BASE}/active/archive`, () =>
        HttpResponse.json(makeList({ id: 2, title: 'Danh sách mua sắm 01/11' }), { status: 201 }),
      ),
    );
    const user = setup();

    await user.click(await screen.findByRole('button', { name: 'Tùy chọn' }));
    await user.click(screen.getByRole('menuitem', { name: 'Lưu trữ danh sách' }));
    const dialog = screen.getByRole('dialog', { name: 'Lưu trữ “Danh sách mua sắm 31/10”?' });
    await user.click(within(dialog).getByRole('button', { name: 'Lưu trữ' }));

    expect(await screen.findByRole('heading', { level: 1, name: 'Danh sách mua sắm 01/11' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Danh sách đang trống' })).toBeInTheDocument();
  });

  it('links to the shopping history', async () => {
    setup();

    expect(await screen.findByRole('link', { name: 'Lịch sử mua sắm' })).toHaveAttribute('href', '/shopping-list/history');
  });

  it('opens the item drawer with its provenance', async () => {
    server.use(http.get(`${BASE}/active/items/beef/provenance`, () => HttpResponse.json(beef.sources)));
    const user = setup();

    await user.click(await screen.findByRole('button', { name: /Thịt bò/ }));

    const drawer = screen.getByRole('dialog', { name: 'Thịt bò' });
    expect(await within(drawer).findByText('Bò kho')).toBeInTheDocument();
  });

  it('shows a capacity error banner when the list is full', async () => {
    server.use(
      http.post(`${BASE}/active/items`, () =>
        HttpResponse.json({ status: 422 }, { status: 422, headers: { 'Content-Type': 'application/problem+json' } }),
      ),
    );
    const user = setup();

    await user.click((await screen.findAllByRole('button', { name: 'Thêm món' }))[0]);
    await user.type(screen.getByLabelText('Tên món'), 'Giấy bạc');
    await user.click(screen.getByRole('button', { name: 'Thêm vào danh sách' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('Danh sách đã đủ 200 món');
  });

  it('offers a retry when the list cannot load', async () => {
    server.use(http.get(`${BASE}/active`, () => HttpResponse.error()));
    render(
      <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <ChecklistScreen />
      </MemoryRouter>,
    );

    const retry = await screen.findByRole('button', { name: 'Thử lại' });
    server.use(http.get(`${BASE}/active`, () => HttpResponse.json(fullList)));
    await userEvent.click(retry);

    await waitFor(() => expect(screen.getByText('Thịt bò')).toBeInTheDocument());
  });
});
