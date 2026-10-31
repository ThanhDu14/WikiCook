import { act, renderHook, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { makeGroup, makeItem, makeList } from '../test/fixtures';
import { useShoppingList } from './useShoppingList';

const BASE = '/api/v1/shopping-lists';

async function renderLoaded() {
  const hook = renderHook(() => useShoppingList());
  await waitFor(() => expect(hook.result.current.list).not.toBeNull());
  return hook;
}

describe('useShoppingList', () => {
  beforeEach(() => {
    server.use(http.get(`${BASE}/active`, () => HttpResponse.json(makeList())));
  });

  it('loads the active list on mount', async () => {
    const { result } = renderHook(() => useShoppingList());

    expect(result.current.loading).toBe(true);
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.list).toMatchObject({ id: 1, title: 'Danh sách mua sắm 31/10' });
    expect(result.current.error).toBeNull();
  });

  it('reports a load error', async () => {
    server.use(http.get(`${BASE}/active`, () => HttpResponse.error()));
    const { result } = renderHook(() => useShoppingList());

    await waitFor(() => expect(result.current.error).not.toBeNull());
    expect(result.current.list).toBeNull();
  });

  it('adds a custom item with a fresh clientUuid and the chosen aisle', async () => {
    let body: Record<string, unknown> = {};
    server.use(
      http.post(`${BASE}/active/items`, async ({ request }) => {
        body = (await request.json()) as Record<string, unknown>;
        return HttpResponse.json(makeList({ aisleGroups: [makeGroup([makeItem()])] }), { status: 201 });
      }),
    );
    const { result } = await renderLoaded();

    await act(() => result.current.addItem({ name: '  Nước mắm ', quantity: 1, unit: 'chai', aisleId: 4 }));

    expect(body).toMatchObject({ customName: 'Nước mắm', quantity: 1, unitName: 'chai', aisleCategoryId: 4 });
    expect(body.clientUuid).toMatch(/^[0-9a-f-]{36}$/);
    expect(result.current.list?.totalItems).toBe(1);
  });

  it('turns the 422 capacity problem into a readable error and keeps the list', async () => {
    server.use(
      http.post(`${BASE}/active/items`, () =>
        HttpResponse.json(
          { status: 422, title: 'Shopping List Capacity Exceeded' },
          { status: 422, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );
    const { result } = await renderLoaded();

    await act(async () => {
      await expect(result.current.addItem({ name: 'Giấy bạc' })).rejects.toThrow();
    });

    expect(result.current.error).toMatch(/200 món/);
    expect(result.current.list).toMatchObject({ id: 1 });
  });

  it('renames the list', async () => {
    server.use(
      http.patch(`${BASE}/active`, async ({ request }) => {
        const { title } = (await request.json()) as { title: string };
        return HttpResponse.json(makeList({ title }));
      }),
    );
    const { result } = await renderLoaded();

    await act(() => result.current.rename('Tiệc cuối tuần'));

    expect(result.current.list?.title).toBe('Tiệc cuối tuần');
  });

  it('deletes an item and reloads the list', async () => {
    let deleted = false;
    server.use(
      http.delete(`${BASE}/active/items/item-1`, () => {
        deleted = true;
        return new HttpResponse(null, { status: 204 });
      }),
      http.get(`${BASE}/active`, () => HttpResponse.json(makeList({ title: deleted ? 'sau khi xóa' : 'trước' }))),
    );
    const { result } = await renderLoaded();

    await act(() => result.current.deleteItem('item-1'));

    expect(deleted).toBe(true);
    expect(result.current.list?.title).toBe('sau khi xóa');
  });

  it('archives into a fresh active list', async () => {
    server.use(http.post(`${BASE}/active/archive`, () => HttpResponse.json(makeList({ id: 2 }), { status: 201 })));
    const { result } = await renderLoaded();

    await act(() => result.current.archive());

    expect(result.current.list?.id).toBe(2);
  });

  it('restores an archived list as the active one', async () => {
    server.use(http.post(`${BASE}/7/restore`, () => HttpResponse.json(makeList({ id: 7, title: 'Đi chợ 20/10' }))));
    const { result } = await renderLoaded();

    await act(() => result.current.restore(7));

    expect(result.current.list).toMatchObject({ id: 7, title: 'Đi chợ 20/10' });
  });

  it('imports a recipe and shows the merged list', async () => {
    server.use(
      http.post(`${BASE}/active/import/recipe`, () =>
        HttpResponse.json(makeList({ aisleGroups: [makeGroup([makeItem({ displayName: 'Thịt bò' })])] })),
      ),
    );
    const { result } = await renderLoaded();

    await act(() => result.current.importRecipe({ recipeId: 1, targetServings: 4, excludedIngredientIds: [] }));

    expect(result.current.list?.aisleGroups[0].items[0].displayName).toBe('Thịt bò');
  });
});
