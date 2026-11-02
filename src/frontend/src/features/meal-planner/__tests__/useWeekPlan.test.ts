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
});
