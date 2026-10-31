import { http, HttpResponse } from 'msw';
import { ApiError } from '@/shared/api/client';
import { server } from '@/test/server';
import { shoppingListApi } from './shoppingListApi';
import { makeList } from '../test/fixtures';

const BASE = '/api/v1/shopping-lists';

describe('shoppingListApi (US1)', () => {
  it('loads the active list', async () => {
    server.use(http.get(`${BASE}/active`, () => HttpResponse.json(makeList())));

    await expect(shoppingListApi.getActive()).resolves.toMatchObject({ id: 1, status: 'ACTIVE' });
  });

  it('renames the active list with PATCH /active', async () => {
    let body: unknown;
    server.use(
      http.patch(`${BASE}/active`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(makeList({ title: 'Tiệc cuối tuần' }));
      }),
    );

    await expect(shoppingListApi.rename('Tiệc cuối tuần')).resolves.toMatchObject({ title: 'Tiệc cuối tuần' });
    expect(body).toEqual({ title: 'Tiệc cuối tuần' });
  });

  it('adds a custom item to the active list', async () => {
    let body: unknown;
    server.use(
      http.post(`${BASE}/active/items`, async ({ request }) => {
        body = await request.json();
        return HttpResponse.json(makeList(), { status: 201 });
      }),
    );

    await shoppingListApi.addCustomItem({ clientUuid: 'u-1', customName: 'Nước mắm', quantity: 1, unitName: 'chai' });

    expect(body).toEqual({ clientUuid: 'u-1', customName: 'Nước mắm', quantity: 1, unitName: 'chai' });
  });

  it('deletes an item by clientUuid', async () => {
    let deleted = '';
    server.use(
      http.delete(`${BASE}/active/items/:clientUuid`, ({ params }) => {
        deleted = String(params.clientUuid);
        return new HttpResponse(null, { status: 204 });
      }),
    );

    await expect(shoppingListApi.deleteItem('u-9')).resolves.toBeUndefined();
    expect(deleted).toBe('u-9');
  });

  it('archives the active list and returns the fresh active list', async () => {
    server.use(http.post(`${BASE}/active/archive`, () => HttpResponse.json(makeList({ id: 2 }), { status: 201 })));

    await expect(shoppingListApi.archive()).resolves.toMatchObject({ id: 2 });
  });

  it('loads archived lists from /history', async () => {
    server.use(
      http.get(`${BASE}/history`, () => HttpResponse.json([makeList({ id: 7, status: 'ARCHIVED', title: 'Chợ Tết' })])),
    );

    await expect(shoppingListApi.getHistory()).resolves.toEqual([
      expect.objectContaining({ id: 7, status: 'ARCHIVED', title: 'Chợ Tết' }),
    ]);
  });

  it('restores an archived list by id', async () => {
    server.use(http.post(`${BASE}/7/restore`, () => HttpResponse.json(makeList({ id: 7 }))));

    await expect(shoppingListApi.restore(7)).resolves.toMatchObject({ id: 7, status: 'ACTIVE' });
  });

  it('surfaces the 422 capacity problem as an ApiError', async () => {
    server.use(
      http.post(`${BASE}/active/items`, () =>
        HttpResponse.json(
          { status: 422, title: 'Shopping List Capacity Exceeded', detail: 'Shopping list capacity limit of 200 items reached' },
          { status: 422, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );

    const error = await shoppingListApi.addCustomItem({ clientUuid: 'u-1', customName: 'x' }).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({ status: 422 });
  });
});

describe('shoppingListApi (US2)', () => {
  it('imports a recipe with an Idempotency-Key header', async () => {
    let body: unknown;
    let key: string | null = null;
    server.use(
      http.post(`${BASE}/active/import/recipe`, async ({ request }) => {
        body = await request.json();
        key = request.headers.get('Idempotency-Key');
        return HttpResponse.json(makeList());
      }),
    );

    await shoppingListApi.importRecipe({ recipeId: 1, targetServings: 4, excludedIngredientIds: [3] }, 'key-123');

    expect(body).toEqual({ recipeId: 1, targetServings: 4, excludedIngredientIds: [3] });
    expect(key).toBe('key-123');
  });

  it('loads the provenance breakdown of an item', async () => {
    server.use(
      http.get(`${BASE}/active/items/u-1/provenance`, () =>
        HttpResponse.json([
          { sourceId: 1, recipeId: 1, mealPlanItemId: null, recipeTitleSnapshot: 'Bò kho', contributedQuantity: 600, unitName: 'g' },
        ]),
      ),
    );

    await expect(shoppingListApi.getProvenance('u-1')).resolves.toEqual([
      expect.objectContaining({ recipeTitleSnapshot: 'Bò kho', contributedQuantity: 600 }),
    ]);
  });
});
