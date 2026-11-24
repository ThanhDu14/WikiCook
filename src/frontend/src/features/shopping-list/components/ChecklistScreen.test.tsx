import { vi } from 'vitest';
import { act, render, screen, waitFor, within } from '@testing-library/react';
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
    expect(screen.queryByRole('progressbar')).not.toBeInTheDocument();
  });

  it('groups items by dish with each dish its own share, typed-in items last', async () => {
    setup();

    const boKho = await screen.findByRole('region', { name: 'Bò kho' });
    expect(within(boKho).getByText('Thịt bò')).toBeInTheDocument();
    expect(within(boKho).getByText('600 g')).toBeInTheDocument();
    expect(within(boKho).getByText('còn 1/1')).toBeInTheDocument();
    expect(within(screen.getByRole('region', { name: 'Bò xào' })).getByText('400 g')).toBeInTheDocument();
    expect(screen.queryByText('600 g Bò kho · 400 g Bò xào')).not.toBeInTheDocument();

    const custom = screen.getByRole('region', { name: 'Món tự thêm' });
    expect(within(custom).getByText('Nước mắm')).toBeInTheDocument();
    expect(within(custom).getByText('Đã đủ')).toBeInTheDocument();
    // a section bought in full goes last
    expect(screen.getAllByRole('region').at(-1)).toBe(custom);
    expect(screen.getByText('Đã mua 1/2 món')).toBeInTheDocument();
    expect(screen.getByText('50%')).toBeInTheDocument();
  });

  it('ticking an ingredient in one dish ticks it in every dish', async () => {
    const user = setup();

    const boKho = await screen.findByRole('region', { name: 'Bò kho' });
    await user.click(within(boKho).getByRole('checkbox', { name: 'Thịt bò' }));

    expect(within(screen.getByRole('region', { name: 'Bò xào' })).getByRole('checkbox', { name: 'Thịt bò' })).toBeChecked();
    expect(screen.getByText('Đã mua 2/2 món')).toBeInTheDocument();
  });

  it('adds a custom item into "Món tự thêm"', async () => {
    const user = setup(makeList());
    server.use(
      http.post(`${BASE}/active/items`, () =>
        HttpResponse.json(makeList({ aisleGroups: [makeGroup([makeItem({ displayName: 'Nước mắm' })])] }), { status: 201 }),
      ),
    );

    await user.click((await screen.findAllByRole('button', { name: 'Thêm món' }))[0]);
    await user.type(screen.getByLabelText('Tên món'), 'Nước mắm');
    await user.click(screen.getByRole('button', { name: 'Thêm vào danh sách' }));

    const custom = await screen.findByRole('region', { name: 'Món tự thêm' });
    expect(within(custom).getByText('Nước mắm')).toBeInTheDocument();
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

    // SL-01 has no rename icon: the title itself is the button
    await user.click(within(await screen.findByRole('heading', { level: 1 })).getByRole('button'));
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

    // SL-01 has no rename icon: the title itself is the button
    await user.click(within(await screen.findByRole('heading', { level: 1 })).getByRole('button'));
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

  it('links to the shopping history from the menu', async () => {
    const user = setup();

    await user.click(await screen.findByRole('button', { name: 'Tùy chọn' }));
    expect(screen.getByRole('menuitem', { name: 'Xem lịch sử' })).toHaveAttribute('href', '/shopping-list/history');
  });

  it('opens the item drawer with its provenance', async () => {
    server.use(http.get(`${BASE}/active/items/beef/provenance`, () => HttpResponse.json(beef.sources)));
    const user = setup();

    await user.click((await screen.findAllByRole('button', { name: /Thịt bò/ }))[0]);

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

    await waitFor(() => expect(screen.getAllByText('Thịt bò').length).toBeGreaterThan(0));
  });

  it('imports a week of the meal plan from the empty list', async () => {
    let body: unknown = null;
    server.use(
      http.post(`${BASE}/active/import/meal-plan`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(fullList);
      }),
    );
    const user = setup(makeList({ aisleGroups: [] }));

    await user.click((await screen.findAllByRole('button', { name: 'Nhập từ thực đơn tuần' }))[0]);
    await user.click(screen.getByRole('button', { name: 'Nhập nguyên liệu' }));

    expect((await screen.findAllByText('Thịt bò')).length).toBe(2);
    expect(body).toMatchObject({ mode: 'MERGE' });
  });

  it('explains an empty week and keeps the dialog open', async () => {
    server.use(
      http.post(`${BASE}/active/import/meal-plan`, () =>
        HttpResponse.json({ status: 422, code: 'EMPTY_MEAL_PLAN', detail: 'empty' }, { status: 422 }),
      ),
    );
    const user = setup(makeList({ aisleGroups: [] }));

    await user.click((await screen.findAllByRole('button', { name: 'Nhập từ thực đơn tuần' }))[0]);
    await user.click(screen.getByRole('button', { name: 'Nhập nguyên liệu' }));

    expect(await screen.findByText('Thực đơn tuần này chưa có món nào để đi chợ.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Nhập nguyên liệu' })).toBeInTheDocument();
  });

  it('copies the list as text', async () => {
    const user = setup();
    const writeText = vi.spyOn(navigator.clipboard, 'writeText').mockResolvedValue(undefined);

    await user.click(await screen.findByRole('button', { name: 'Tùy chọn' }));
    await user.click(screen.getByRole('menuitem', { name: 'Sao chép' }));

    expect(writeText).toHaveBeenCalledWith(expect.stringContaining('Bò kho\n[ ] Thịt bò — 600 g'));
    expect(await screen.findByText(/Đã sao chép danh sách/)).toBeInTheDocument();
  });

  it('shows six sections a page, with the recipe photo on its dish', async () => {
    const items = Array.from({ length: 8 }, (_, n) =>
      makeItem({
        clientUuid: `i${n}`,
        displayName: `Nguyên liệu ${n}`,
        sources: [
          { sourceId: n, recipeId: n + 1, mealPlanItemId: n + 1, recipeTitleSnapshot: `Món ${n + 1}`, contributedQuantity: 1, unitName: 'g' },
        ],
      }),
    );
    const user = setup(makeList({ aisleGroups: [makeGroup(items)], recipeCovers: { '1': 'https://img/mon-1.jpg' } }));

    expect(await screen.findByRole('region', { name: 'Món 1' })).toBeInTheDocument();
    expect(screen.getAllByRole('region')).toHaveLength(6);
    expect(screen.getByRole('img', { name: 'Món 1' })).toHaveAttribute('src', 'https://img/mon-1.jpg');
    expect(screen.queryByRole('region', { name: 'Món 7' })).not.toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: '2' }));
    expect(screen.getAllByRole('region').map((r) => r.getAttribute('aria-label'))).toEqual(['Món 7', 'Món 8']);
  });

  it('prints every section, not only the current page', async () => {
    const items = Array.from({ length: 8 }, (_, n) =>
      makeItem({
        clientUuid: `i${n}`,
        displayName: `Nguyên liệu ${n}`,
        sources: [
          { sourceId: n, recipeId: n + 1, mealPlanItemId: n + 1, recipeTitleSnapshot: `Món ${n + 1}`, contributedQuantity: 1, unitName: 'g' },
        ],
      }),
    );
    setup(makeList({ aisleGroups: [makeGroup(items)] }));
    await screen.findByRole('region', { name: 'Món 1' });

    act(() => {
      window.dispatchEvent(new Event('beforeprint'));
    });
    expect(screen.getAllByRole('region')).toHaveLength(8);
    act(() => {
      window.dispatchEvent(new Event('afterprint'));
    });
    expect(screen.getAllByRole('region')).toHaveLength(6);
  });
});
