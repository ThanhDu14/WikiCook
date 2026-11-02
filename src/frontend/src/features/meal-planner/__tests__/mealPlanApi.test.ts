import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { mealPlanApi, MealPlanApiError } from '../api/mealPlanApi';

const problem = (status: number, body: Record<string, unknown>) =>
  HttpResponse.json({ status, ...body }, { status, headers: { 'Content-Type': 'application/problem+json' } });

describe('mealPlanApi (contracts/openapi.yaml)', () => {
  it('calls the documented method and URL for every operation', async () => {
    const seen: string[] = [];
    const record = (label: string, body: Record<string, unknown> = {}) => async ({ request }: { request: Request }) => {
      const url = new URL(request.url);
      const sent = request.method === 'GET' || request.method === 'DELETE' ? null : await request.json();
      seen.push(`${request.method} ${url.pathname}${url.search} ${sent ? JSON.stringify(sent) : ''}`.trim());
      return label === '204' ? new HttpResponse(null, { status: 204 }) : HttpResponse.json(body);
    };
    server.use(
      http.get('/api/v1/meal-plans/picker/recipes', record('picker', { content: [], page: 0, size: 20, hasNext: false })),
      http.get('/api/v1/meal-plans/:week', record('week')),
      http.put('/api/v1/meal-plans/:week/slots/:day/:meal', record('put')),
      http.patch('/api/v1/meal-plans/:week/slots/:day/:meal/servings', record('patch')),
      http.post('/api/v1/meal-plans/:week/moves', record('move')),
      http.delete('/api/v1/meal-plans/:week/slots/:day/:meal', record('204')),
      http.delete('/api/v1/meal-plans/:week/days/:day', record('204')),
      http.get('/api/v1/meal-plans/:week/shopping-handoff', record('handoff')),
    );

    await mealPlanApi.getWeek('2026-10-19');
    await mealPlanApi.putSlot('2026-10-19', 'MONDAY', 'LUNCH', { recipeId: 5, expected: null });
    await mealPlanApi.patchServings('2026-10-19', 'MONDAY', 'LUNCH', { servings: 3, expected: { itemId: 1, version: 0 } });
    await mealPlanApi.moveItem('2026-10-19', {
      from: { day: 'MONDAY', meal: 'LUNCH', itemId: 1, version: 1 },
      to: { day: 'TUESDAY', meal: 'DINNER', expected: null },
    });
    await mealPlanApi.deleteSlot('2026-10-19', 'MONDAY', 'LUNCH', { itemId: 1, version: 2 });
    await mealPlanApi.clearDay('2026-10-19', 'SUNDAY');
    await mealPlanApi.getShoppingHandoff('2026-10-19');
    await mealPlanApi.searchPickerRecipes({ meal: 'BREAKFAST', q: 'trứng', scope: 'COLLECTIONS', page: 1 });

    expect(seen).toEqual([
      'GET /api/v1/meal-plans/2026-10-19',
      'PUT /api/v1/meal-plans/2026-10-19/slots/MONDAY/LUNCH {"recipeId":5,"expected":null}',
      'PATCH /api/v1/meal-plans/2026-10-19/slots/MONDAY/LUNCH/servings {"servings":3,"expected":{"itemId":1,"version":0}}',
      'POST /api/v1/meal-plans/2026-10-19/moves {"from":{"day":"MONDAY","meal":"LUNCH","itemId":1,"version":1},"to":{"day":"TUESDAY","meal":"DINNER","expected":null}}',
      'DELETE /api/v1/meal-plans/2026-10-19/slots/MONDAY/LUNCH?itemId=1&version=2',
      'DELETE /api/v1/meal-plans/2026-10-19/days/SUNDAY',
      'GET /api/v1/meal-plans/2026-10-19/shopping-handoff',
      'GET /api/v1/meal-plans/picker/recipes?meal=BREAKFAST&q=tr%E1%BB%A9ng&scope=COLLECTIONS&page=1',
    ]);
  });

  it('turns a ProblemDetail into a MealPlanApiError with code and currentItem', async () => {
    const currentItem = { id: 9, version: 2, recipeName: 'Phở bò', servings: 2, source: 'MANUAL', available: true };
    server.use(
      http.put('/api/v1/meal-plans/:week/slots/:day/:meal', () =>
        problem(409, { code: 'SLOT_OCCUPIED', title: 'Conflict', currentItem }),
      ),
    );

    const error = await mealPlanApi
      .putSlot('2026-10-19', 'WEDNESDAY', 'LUNCH', { recipeId: 5, expected: null })
      .catch((e: unknown) => e);

    expect(error).toBeInstanceOf(MealPlanApiError);
    expect(error).toMatchObject({ status: 409, code: 'SLOT_OCCUPIED', currentItem });
  });

  it('uses UNKNOWN when the problem has no code', async () => {
    server.use(http.get('/api/v1/meal-plans/:week', () => problem(500, { title: 'Internal Server Error' })));

    await expect(mealPlanApi.getWeek('2026-10-19')).rejects.toMatchObject({ status: 500, code: 'UNKNOWN' });
  });

  it('turns a network failure into code NETWORK', async () => {
    server.use(http.get('/api/v1/meal-plans/:week', () => HttpResponse.error()));

    await expect(mealPlanApi.getWeek('2026-10-19')).rejects.toMatchObject({ status: 0, code: 'NETWORK' });
  });
});
