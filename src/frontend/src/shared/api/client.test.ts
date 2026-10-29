import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { api, ApiError } from './client';

describe('api client', () => {
  it('prefixes /api/v1 and returns the JSON body', async () => {
    server.use(http.get('/api/v1/shopping-lists/active', () => HttpResponse.json({ id: 'abc', items: [] })));

    await expect(api.get('/shopping-lists/active')).resolves.toEqual({ id: 'abc', items: [] });
  });

  it('sends JSON bodies with a Content-Type header', async () => {
    server.use(
      http.post('/api/v1/items', async ({ request }) =>
        HttpResponse.json({ contentType: request.headers.get('content-type'), body: await request.json() }),
      ),
    );

    await expect(api.post('/items', { name: 'Hành lá' })).resolves.toEqual({
      contentType: 'application/json',
      body: { name: 'Hành lá' },
    });
  });

  it('returns undefined for 204 No Content', async () => {
    server.use(http.delete('/api/v1/items/1', () => new HttpResponse(null, { status: 204 })));

    await expect(api.delete('/items/1')).resolves.toBeUndefined();
  });

  it('turns an RFC 7807 problem into an ApiError', async () => {
    server.use(
      http.post('/api/v1/items', () =>
        HttpResponse.json(
          { type: 'about:blank', title: 'Unprocessable Entity', status: 422, detail: 'List is full (200 items)' },
          { status: 422, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );

    const error = await api.post('/items', { name: 'x' }).catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect(error).toMatchObject({
      status: 422,
      message: 'List is full (200 items)',
      problem: { title: 'Unprocessable Entity', detail: 'List is full (200 items)' },
    });
  });

  it('builds a problem from the status when the error body is not JSON', async () => {
    server.use(
      http.get('/api/v1/items', () => new HttpResponse('<html>Bad Gateway</html>', { status: 502, statusText: 'Bad Gateway' })),
    );

    await expect(api.get('/items')).rejects.toMatchObject({ status: 502, problem: { title: 'Bad Gateway' } });
  });
});
