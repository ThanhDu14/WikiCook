import { act, renderHook, waitFor } from '@testing-library/react';
import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { useWeekPlan } from '../hooks/useWeekPlan';
import { vi as strings } from '../strings.vi';
import { item, week, WEEK } from './fixtures';

const pho = { recipeId: 7, title: 'Phở bò', coverImageUrl: null, totalTimeMinutes: 45 };

describe('useWeekPlan (T034)', () => {
  it('loads the week', async () => {
    server.use(http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': item() }))));

    const { result } = renderHook(() => useWeekPlan(WEEK));

    expect(result.current.status).toBe('loading');
    await waitFor(() => expect(result.current.status).toBe('ready'));
    expect(result.current.week?.days[0].slots[1].item?.recipeName).toBe('Canh chua cá lóc');
  });

  it('adds a dish at once, then keeps the item the server saved', async () => {
    let release!: () => void;
    const saved = new Promise<void>((resolve) => (release = resolve));
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week())),
      http.put(`/api/v1/meal-plans/${WEEK}/slots/TUESDAY/BREAKFAST`, async () => {
        await saved;
        return HttpResponse.json(item({ id: 99, recipeId: 7, recipeName: 'Phở bò', servings: 2 }));
      }),
    );
    const { result } = renderHook(() => useWeekPlan(WEEK));
    await waitFor(() => expect(result.current.status).toBe('ready'));

    let done!: Promise<unknown>;
    act(() => {
      done = result.current.addDish('TUESDAY', 'BREAKFAST', pho);
    });

    const optimistic = result.current.week?.days[1].slots[0].item;
    expect(optimistic?.recipeName).toBe('Phở bò'); // shown before the server answers
    release();
    await act(async () => {
      await done;
    });
    expect(result.current.week?.days[1].slots[0].item).toMatchObject({ id: 99, servings: 2 });
    expect(result.current.week?.empty).toBe(false);
  });

  it('puts the week back and reports the error when saving fails (FR-019)', async () => {
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week())),
      http.put(`/api/v1/meal-plans/${WEEK}/slots/TUESDAY/BREAKFAST`, () =>
        HttpResponse.json({ status: 500, title: 'Internal Server Error' }, { status: 500 }),
      ),
    );
    const { result } = renderHook(() => useWeekPlan(WEEK));
    await waitFor(() => expect(result.current.status).toBe('ready'));

    let outcome: unknown;
    await act(async () => {
      outcome = await result.current.addDish('TUESDAY', 'BREAKFAST', pho);
    });

    expect(outcome).toBe('UNKNOWN');
    expect(result.current.week?.days[1].slots[0].item).toBeNull();
    expect(result.current.notice).toBe(strings.notSaved);
  });

  it('changes servings and rolls back on error', async () => {
    const current = item({ id: 5, version: 0, servings: 4 });
    const sent: unknown[] = [];
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': current }))),
      http.patch(`/api/v1/meal-plans/${WEEK}/slots/MONDAY/LUNCH/servings`, async ({ request }) => {
        const body = (await request.json()) as { servings: number; expected: unknown };
        sent.push(body.expected);
        return body.servings === 6
          ? HttpResponse.json({ ...current, servings: 6, version: 1 })
          : HttpResponse.json({ status: 500 }, { status: 500 });
      }),
    );
    const { result } = renderHook(() => useWeekPlan(WEEK));
    await waitFor(() => expect(result.current.status).toBe('ready'));

    await act(async () => {
      await result.current.changeServings('MONDAY', 'LUNCH', 6);
    });
    expect(result.current.week?.days[0].slots[1].item).toMatchObject({ servings: 6, version: 1 });

    await act(async () => {
      await result.current.changeServings('MONDAY', 'LUNCH', 7);
    });
    expect(result.current.week?.days[0].slots[1].item).toMatchObject({ servings: 6, version: 1 });
    expect(result.current.notice).toBe(strings.notSaved);
    expect(sent).toEqual([{ itemId: 5, version: 0 }, { itemId: 5, version: 1 }]);
  });

  it('reloads the week when the slot changed elsewhere (409)', async () => {
    let loads = 0;
    server.use(
      http.get(`/api/v1/meal-plans/${WEEK}`, () => {
        loads += 1;
        return HttpResponse.json(loads === 1 ? week() : week({ 'TUESDAY/BREAKFAST': item({ recipeName: 'Bún chả' }) }));
      }),
      http.put(`/api/v1/meal-plans/${WEEK}/slots/TUESDAY/BREAKFAST`, () =>
        HttpResponse.json({ status: 409, code: 'SLOT_CHANGED' }, { status: 409 }),
      ),
    );
    const { result } = renderHook(() => useWeekPlan(WEEK));
    await waitFor(() => expect(result.current.status).toBe('ready'));

    await act(async () => {
      await result.current.addDish('TUESDAY', 'BREAKFAST', pho);
    });

    await waitFor(() => expect(result.current.week?.days[1].slots[0].item?.recipeName).toBe('Bún chả'));
    expect(result.current.notice).toBe(strings.planChanged);
  });

  it('has no separate save step (US1-9)', () => {
    server.use(http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week())));
    const { result } = renderHook(() => useWeekPlan(WEEK));

    expect(Object.keys(result.current)).not.toContain('save');
  });

  describe('rearranging (T063, US3-7)', () => {
    const pho2 = item({ id: 5, version: 1, recipeId: 7, recipeName: 'Phở bò', servings: 2 });
    const ca = item({ id: 6, version: 3, recipeId: 8, recipeName: 'Cá kho', servings: 5 });
    const fail = () => HttpResponse.json({ status: 500, title: 'Internal Server Error' }, { status: 500 });

    async function ready() {
      const hook = renderHook(() => useWeekPlan(WEEK));
      await waitFor(() => expect(hook.result.current.status).toBe('ready'));
      return hook.result;
    }

    const at = (result: { current: ReturnType<typeof useWeekPlan> }, day: number, meal: number) =>
      result.current.week?.days[day].slots[meal].item ?? null;

    it('moves to an empty slot and swaps with a full one', async () => {
      const bodies: unknown[] = [];
      server.use(
        http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(week({ 'MONDAY/LUNCH': pho2, 'MONDAY/DINNER': ca }))),
        http.post(`/api/v1/meal-plans/${WEEK}/moves`, async ({ request }) => {
          const body = (await request.json()) as { to: { day: string; meal: string } };
          bodies.push(body);
          return bodies.length === 1
            ? HttpResponse.json({ kind: 'MOVED', items: [{ day: 'FRIDAY', meal: 'BREAKFAST', item: { ...pho2, version: 2 } }] })
            : HttpResponse.json({
                kind: 'SWAPPED',
                items: [
                  { day: 'FRIDAY', meal: 'BREAKFAST', item: { ...ca, id: 5, version: 3 } },
                  { day: 'MONDAY', meal: 'DINNER', item: { ...pho2, id: 6, version: 4 } },
                ],
              });
        }),
      );
      const result = await ready();

      await act(async () => {
        await result.current.moveItem({ day: 'MONDAY', meal: 'LUNCH' }, { day: 'FRIDAY', meal: 'BREAKFAST' });
      });
      expect(at(result, 0, 1)).toBeNull();
      expect(at(result, 4, 0)).toMatchObject({ id: 5, version: 2, recipeName: 'Phở bò' });

      await act(async () => {
        await result.current.moveItem({ day: 'FRIDAY', meal: 'BREAKFAST' }, { day: 'MONDAY', meal: 'DINNER' });
      });
      expect(at(result, 4, 0)).toMatchObject({ id: 5, recipeName: 'Cá kho' });
      expect(at(result, 0, 2)).toMatchObject({ id: 6, recipeName: 'Phở bò' });
      expect(bodies).toEqual([
        { from: { day: 'MONDAY', meal: 'LUNCH', itemId: 5, version: 1 }, to: { day: 'FRIDAY', meal: 'BREAKFAST', expected: null } },
        { from: { day: 'FRIDAY', meal: 'BREAKFAST', itemId: 5, version: 2 }, to: { day: 'MONDAY', meal: 'DINNER', expected: { itemId: 6, version: 3 } } },
      ]);
    });

    it.each([
      ['move', (r: ReturnType<typeof useWeekPlan>) => r.moveItem({ day: 'MONDAY', meal: 'LUNCH' }, { day: 'MONDAY', meal: 'DINNER' })],
      ['remove', (r: ReturnType<typeof useWeekPlan>) => r.removeDish('MONDAY', 'LUNCH')],
      ['clear day', (r: ReturnType<typeof useWeekPlan>) => r.clearDay('MONDAY')],
    ])('puts the week back when %s fails (FR-019)', async (_name, change) => {
      const plan = week({ 'MONDAY/LUNCH': pho2, 'MONDAY/DINNER': ca });
      server.use(
        http.get(`/api/v1/meal-plans/${WEEK}`, () => HttpResponse.json(plan)),
        http.post(`/api/v1/meal-plans/${WEEK}/moves`, fail),
        http.delete(`/api/v1/meal-plans/${WEEK}/slots/MONDAY/LUNCH`, fail),
        http.delete(`/api/v1/meal-plans/${WEEK}/days/MONDAY`, fail),
      );
      const result = await ready();

      await act(async () => {
        await change(result.current);
      });

      expect(result.current.week).toEqual(plan);
      expect(result.current.notice).toBe(strings.notSaved);
    });

    it('removes a dish and clears a day', async () => {
      let removed = '';
      server.use(
        http.get(`/api/v1/meal-plans/${WEEK}`, () =>
          HttpResponse.json(week({ 'MONDAY/LUNCH': pho2, 'TUESDAY/DINNER': ca, 'TUESDAY/LUNCH': item({ id: 9 }) })),
        ),
        http.delete(`/api/v1/meal-plans/${WEEK}/slots/MONDAY/LUNCH`, ({ request }) => {
          removed = new URL(request.url).search;
          return new HttpResponse(null, { status: 204 });
        }),
        http.delete(`/api/v1/meal-plans/${WEEK}/days/TUESDAY`, () => new HttpResponse(null, { status: 204 })),
      );
      const result = await ready();

      await act(async () => {
        await result.current.removeDish('MONDAY', 'LUNCH');
        await result.current.clearDay('TUESDAY');
      });

      expect(removed).toBe('?itemId=5&version=1');
      expect(result.current.week?.empty).toBe(true);
    });

    it('reloads the week when a move hits a change made elsewhere (409)', async () => {
      let loads = 0;
      server.use(
        http.get(`/api/v1/meal-plans/${WEEK}`, () => {
          loads += 1;
          return HttpResponse.json(week({ 'MONDAY/LUNCH': loads === 1 ? pho2 : ca }));
        }),
        http.post(`/api/v1/meal-plans/${WEEK}/moves`, () => HttpResponse.json({ status: 409, code: 'SLOT_CHANGED' }, { status: 409 })),
      );
      const result = await ready();

      await act(async () => {
        await result.current.moveItem({ day: 'MONDAY', meal: 'LUNCH' }, { day: 'SUNDAY', meal: 'LUNCH' });
      });

      await waitFor(() => expect(at(result, 0, 1)?.recipeName).toBe('Cá kho'));
      expect(result.current.notice).toBe(strings.planChanged);
    });
  });
});
