import { act, renderHook, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { useWeekPlan } from '../hooks/useWeekPlan';
import { vi as strings } from '../strings.vi';
import { item, week, WEEK } from './fixtures';

const pho = { recipeId: 7, title: 'Phở bò', coverImageUrl: null, totalTimeMinutes: 45 };
const fail = () => HttpResponse.json({ status: 500, title: 'Internal Server Error' }, { status: 500 });

type Hook = { current: ReturnType<typeof useWeekPlan> };

async function ready() {
  const hook = renderHook(() => useWeekPlan(WEEK));
  await waitFor(() => expect(hook.result.current.status).toBe('ready'));
  return hook.result;
}

/** Names of the dishes of a meal, in order. */
const names = (result: Hook, day: number, meal: number) =>
  result.current.week?.days[day].slots[meal].items.map((i) => i.recipeName) ?? [];

describe('useWeekPlan (T034, T115)', () => {
  it('loads the week', async () => {
    server.use(http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': item() }))));

    const { result } = renderHook(() => useWeekPlan(WEEK));

    expect(result.current.status).toBe('loading');
    await waitFor(() => expect(result.current.status).toBe('ready'));
    expect(names(result, 0, 1)).toEqual(['Canh chua cá lóc']);
  });

  it('adds a dish at the end of the meal at once, then keeps the item the server saved', async () => {
    let release!: () => void;
    const saved = new Promise<void>((resolve) => (release = resolve));
    let sent: unknown;
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'TUESDAY/BREAKFAST': item({ id: 3 }) }))),
      http.post(`/api/v1/meal-plans/${WEEK}/meals/TUESDAY/BREAKFAST/items`, async ({ request }) => {
        sent = await request.json();
        await saved;
        return HttpResponse.json(
          { day: 'TUESDAY', meal: 'BREAKFAST', item: item({ id: 99, position: 1, recipeId: 7, recipeName: 'Phở bò', servings: 2 }) },
          { status: 201 },
        );
      }),
    );
    const result = await ready();

    let done!: Promise<unknown>;
    act(() => {
      done = result.current.addDish('TUESDAY', 'BREAKFAST', pho);
    });

    expect(names(result, 1, 0)).toEqual(['Canh chua cá lóc', 'Phở bò']); // shown before the server answers
    release();
    await act(async () => {
      await done;
    });
    expect(result.current.week?.days[1].slots[0].items[1]).toMatchObject({ id: 99, servings: 2, position: 1 });
    expect(result.current.week?.availableItemCount).toBe(2);
    expect(sent).toEqual({ recipeId: 7, servings: null });
  });

  it('refuses a fifth dish without calling the server (FR-004)', async () => {
    let calls = 0;
    const four = [1, 2, 3, 4].map((id) => item({ id, recipeName: `Món ${id}` }));
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/DINNER': four }))),
      http.post(`/api/v1/meal-plans/${WEEK}/meals/MONDAY/DINNER/items`, () => {
        calls += 1;
        return HttpResponse.json({ status: 409, code: 'MEAL_FULL' }, { status: 409 });
      }),
    );
    const result = await ready();

    let outcome: unknown;
    await act(async () => {
      outcome = await result.current.addDish('MONDAY', 'DINNER', pho);
    });

    expect(outcome).toBe('MEAL_FULL');
    expect(calls).toBe(0);
    expect(names(result, 0, 2)).toHaveLength(4);
    expect(result.current.notice).toBe(strings.mealFull);
  });

  it('puts the week back and reports the error when saving fails (FR-019)', async () => {
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week())),
      http.post(`/api/v1/meal-plans/${WEEK}/meals/TUESDAY/BREAKFAST/items`, fail),
    );
    const result = await ready();

    let outcome: unknown;
    await act(async () => {
      outcome = await result.current.addDish('TUESDAY', 'BREAKFAST', pho);
    });

    expect(outcome).toBe('UNKNOWN');
    expect(names(result, 1, 0)).toEqual([]);
    expect(result.current.notice).toBe(strings.notSaved);
  });

  it('replaces one dish and keeps its servings and place (US3-5)', async () => {
    let sent: unknown;
    const canh = item({ id: 5, version: 2, servings: 3 });
    const ca = item({ id: 6, recipeName: 'Cá kho' });
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/DINNER': [canh, ca] }))),
      http.put(`/api/v1/meal-plans/${WEEK}/items/5`, async ({ request }) => {
        sent = await request.json();
        return HttpResponse.json({ day: 'MONDAY', meal: 'DINNER', item: { ...canh, recipeId: 7, recipeName: 'Phở bò', version: 3 } });
      }),
    );
    const result = await ready();

    await act(async () => {
      await result.current.replaceDish(5, pho);
    });

    expect(names(result, 0, 2)).toEqual(['Phở bò', 'Cá kho']);
    expect(result.current.week?.days[0].slots[2].items[0]).toMatchObject({ id: 5, version: 3, servings: 3 });
    expect(sent).toEqual({ recipeId: 7, servings: null, version: 2 });
  });

  it('changes servings and rolls back on error', async () => {
    const current = item({ id: 5, version: 0, servings: 4 });
    const sent: unknown[] = [];
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': current }))),
      http.patch(`/api/v1/meal-plans/${WEEK}/items/5/servings`, async ({ request }) => {
        const body = (await request.json()) as { servings: number; version: number };
        sent.push(body);
        return body.servings === 6
          ? HttpResponse.json({ day: 'MONDAY', meal: 'LUNCH', item: { ...current, servings: 6, version: 1 } })
          : fail();
      }),
    );
    const result = await ready();

    await act(async () => {
      await result.current.changeServings(5, 6);
    });
    expect(result.current.week?.days[0].slots[1].items[0]).toMatchObject({ servings: 6, version: 1 });

    await act(async () => {
      await result.current.changeServings(5, 7);
    });
    expect(result.current.week?.days[0].slots[1].items[0]).toMatchObject({ servings: 6, version: 1 });
    expect(result.current.notice).toBe(strings.notSaved);
    expect(sent).toEqual([{ servings: 6, version: 0 }, { servings: 7, version: 1 }]);
  });

  it('reloads the week when the dish changed elsewhere (409)', async () => {
    let loads = 0;
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => {
        loads += 1;
        return HttpResponse.json(loads === 1 ? week() : week({ 'TUESDAY/BREAKFAST': item({ recipeName: 'Bún chả' }) }));
      }),
      http.post(`/api/v1/meal-plans/${WEEK}/meals/TUESDAY/BREAKFAST/items`, () =>
        HttpResponse.json({ status: 409, code: 'SLOT_CHANGED' }, { status: 409 }),
      ),
    );
    const result = await ready();

    await act(async () => {
      await result.current.addDish('TUESDAY', 'BREAKFAST', pho);
    });

    await waitFor(() => expect(names(result, 1, 0)).toEqual(['Bún chả']));
    expect(result.current.notice).toBe(strings.planChanged);
  });

  it('has no separate save step (US1-9)', () => {
    server.use(http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week())));
    const { result } = renderHook(() => useWeekPlan(WEEK));

    expect(Object.keys(result.current)).not.toContain('save');
  });

  describe('rearranging (T063, T115, US3)', () => {
    const canh = item({ id: 5, version: 1, recipeId: 7, recipeName: 'Canh chua' });
    const ca = item({ id: 6, version: 3, recipeId: 8, recipeName: 'Cá kho' });
    const rau = item({ id: 7, version: 0, recipeId: 9, recipeName: 'Rau muống' });

    it('moves a dish to the end of another meal and takes what the server returned', async () => {
      let body: unknown;
      server.use(
        http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': [canh, ca], 'FRIDAY/DINNER': rau }))),
        http.post(`/api/v1/meal-plans/${WEEK}/items/5/move`, async ({ request }) => {
          body = await request.json();
          return HttpResponse.json({
            meals: [
              { day: 'MONDAY', meal: 'LUNCH', items: [{ ...ca, position: 0, version: 4 }] },
              { day: 'FRIDAY', meal: 'DINNER', items: [{ ...rau, position: 0 }, { ...canh, position: 1, version: 2 }] },
            ],
          });
        }),
      );
      const result = await ready();

      await act(async () => {
        await result.current.moveItem(5, { day: 'FRIDAY', meal: 'DINNER' });
      });

      expect(names(result, 0, 1)).toEqual(['Cá kho']);
      expect(names(result, 4, 2)).toEqual(['Rau muống', 'Canh chua']);
      expect(result.current.week?.days[0].slots[1].items[0]).toMatchObject({ version: 4, position: 0 });
      expect(body).toEqual({ day: 'FRIDAY', meal: 'DINNER', position: null, version: 1 });
    });

    it('moves a dish up and down inside its meal ("Lên trên", "Xuống dưới")', async () => {
      const bodies: unknown[] = [];
      let release!: () => void;
      const answered = new Promise<void>((resolve) => (release = resolve));
      server.use(
        http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/DINNER': [canh, ca, rau] }))),
        http.post(`/api/v1/meal-plans/${WEEK}/items/6/move`, async ({ request }) => {
          bodies.push(await request.json());
          await answered;
          return HttpResponse.json({
            meals: [{ day: 'MONDAY', meal: 'DINNER', items: [{ ...ca, position: 0, version: 4 }, { ...canh, position: 1 }, { ...rau, position: 2 }] }],
          });
        }),
      );
      const result = await ready();

      let done!: Promise<unknown>;
      act(() => {
        done = result.current.moveItem(6, { day: 'MONDAY', meal: 'DINNER', position: 0 });
      });
      expect(names(result, 0, 2)).toEqual(['Cá kho', 'Canh chua', 'Rau muống']); // at once
      release();
      await act(async () => {
        await done;
      });

      expect(result.current.week?.days[0].slots[2].items.map((i) => i.position)).toEqual([0, 1, 2]);
      expect(bodies).toEqual([{ day: 'MONDAY', meal: 'DINNER', position: 0, version: 3 }]);
    });

    it('does not move into a full meal (FR-004)', async () => {
      let calls = 0;
      const four = [11, 12, 13, 14].map((id) => item({ id, recipeName: `Món ${id}` }));
      server.use(
        http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': canh, 'TUESDAY/DINNER': four }))),
        http.post(`/api/v1/meal-plans/${WEEK}/items/5/move`, () => {
          calls += 1;
          return HttpResponse.json({ meals: [] });
        }),
      );
      const result = await ready();

      let outcome: unknown;
      await act(async () => {
        outcome = await result.current.moveItem(5, { day: 'TUESDAY', meal: 'DINNER' });
      });

      expect(outcome).toBe('MEAL_FULL');
      expect(calls).toBe(0);
      expect(result.current.notice).toBe(strings.mealFull);
      expect(names(result, 0, 1)).toEqual(['Canh chua']);
    });

    it.each([
      ['move', (r: ReturnType<typeof useWeekPlan>) => r.moveItem(5, { day: 'MONDAY', meal: 'DINNER' })],
      ['remove', (r: ReturnType<typeof useWeekPlan>) => r.removeDish(5)],
      ['clear day', (r: ReturnType<typeof useWeekPlan>) => r.clearDay('MONDAY')],
    ])('puts the week back when %s fails (FR-019)', async (_name, change) => {
      const plan = week({ 'MONDAY/LUNCH': [canh, ca], 'MONDAY/DINNER': rau });
      server.use(
        http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(plan)),
        http.post(`/api/v1/meal-plans/${WEEK}/items/5/move`, fail),
        http.delete(`/api/v1/meal-plans/${WEEK}/items/5`, fail),
        http.delete(`/api/v1/meal-plans/${WEEK}/days/MONDAY`, fail),
      );
      const result = await ready();

      await act(async () => {
        await change(result.current);
      });

      expect(result.current.week).toEqual(plan);
      expect(result.current.notice).toBe(strings.notSaved);
    });

    it('removes one dish, the later ones move up, and clears a day', async () => {
      let removed = '';
      server.use(
        http.get(`/api/v1/meal-plans/${WEEK}`, () =>
          HttpResponse.json(week({ 'MONDAY/LUNCH': [canh, ca], 'TUESDAY/DINNER': rau, 'TUESDAY/LUNCH': item({ id: 9 }) })),
        ),
        http.delete(`/api/v1/meal-plans/${WEEK}/items/5`, ({ request }) => {
          removed = new URL(request.url).search;
          return new HttpResponse(null, { status: 204 });
        }),
        http.delete(`/api/v1/meal-plans/${WEEK}/days/TUESDAY`, () => new HttpResponse(null, { status: 204 })),
      );
      const result = await ready();

      await act(async () => {
        await result.current.removeDish(5);
      });
      expect(names(result, 0, 1)).toEqual(['Cá kho']);
      expect(result.current.week?.days[0].slots[1].items[0].position).toBe(0);

      await act(async () => {
        await result.current.clearDay('TUESDAY');
      });
      expect(removed).toBe('?version=1');
      expect(result.current.week?.availableItemCount).toBe(1);
      expect(result.current.week?.empty).toBe(false);
    });

    it('reloads the week when a move hits a change made elsewhere (409)', async () => {
      let loads = 0;
      server.use(
        http.get(`/api/v1/meal-plans/${WEEK}`, () => {
          loads += 1;
          return HttpResponse.json(week({ 'MONDAY/LUNCH': loads === 1 ? canh : ca }));
        }),
        http.post(`/api/v1/meal-plans/${WEEK}/items/5/move`, () => HttpResponse.json({ status: 409, code: 'SLOT_CHANGED' }, { status: 409 })),
      );
      const result = await ready();

      await act(async () => {
        await result.current.moveItem(5, { day: 'SUNDAY', meal: 'LUNCH' });
      });

      await waitFor(() => expect(names(result, 0, 1)).toEqual(['Cá kho']));
      expect(result.current.notice).toBe(strings.planChanged);
    });
  });
});
