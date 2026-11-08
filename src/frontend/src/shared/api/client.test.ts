import { http, HttpResponse } from 'msw';
import { server } from '@/test/server';
import { api, ApiError } from './client';
import { GUEST, setAccessToken, setDevUserId } from './session';
import { installMemoryStorage } from './memoryStorage.testutil';

beforeAll(() => {
  installMemoryStorage();
});

/** Echoes the identity headers the backend reads (ACT-01). */
const echoHeaders = ({ request }: { request: Request }) =>
  HttpResponse.json({
    userId: request.headers.get('x-user-id'),
    devUserId: request.headers.get('x-dev-user-id'),
    authorization: request.headers.get('authorization'),
    contentType: request.headers.get('content-type'),
  });

describe('api client identity headers (ACT-01)', () => {
  afterEach(() => {
    window.localStorage.clear();
  });

  it('sends the default dev user 1 in X-User-Id and X-Dev-User-Id, no Authorization', async () => {
    server.use(http.get('/api/v1/whoami', echoHeaders));

    await expect(api.get('/whoami')).resolves.toMatchObject({ userId: '1', devUserId: '1', authorization: null });
  });

  it('sends the dev user chosen in the session', async () => {
    setDevUserId(5);
    server.use(http.get('/api/v1/whoami', echoHeaders));

    await expect(api.get('/whoami')).resolves.toMatchObject({ userId: '5', devUserId: '5' });
  });

  it('sends no user headers when the dev user is a guest', async () => {
    setDevUserId(GUEST);
    server.use(http.get('/api/v1/whoami', echoHeaders));

    await expect(api.get('/whoami')).resolves.toMatchObject({ userId: null, devUserId: null });
  });

  it('sends Authorization: Bearer when a token is stored', async () => {
    setAccessToken('abc.def.ghi');
    server.use(http.get('/api/v1/whoami', echoHeaders));

    await expect(api.get('/whoami')).resolves.toMatchObject({ authorization: 'Bearer abc.def.ghi' });
  });

  it('upload() posts multipart form data with the same headers and no JSON content type', async () => {
    setDevUserId(5);
    setAccessToken('tok');
    server.use(
      http.post('/api/v1/recipes/9/cover', async ({ request }) => {
        const form = await request.formData();
        const file = form.get('file') as File;
        return HttpResponse.json({
          devUserId: request.headers.get('x-dev-user-id'),
          authorization: request.headers.get('authorization'),
          multipart: (request.headers.get('content-type') ?? '').startsWith('multipart/form-data'),
          fileName: file.name,
        });
      }),
    );
    const form = new FormData();
    form.append('file', new File(['x'], 'cover.jpg', { type: 'image/jpeg' }));

    await expect(api.upload('/recipes/9/cover', form)).resolves.toEqual({
      devUserId: '5',
      authorization: 'Bearer tok',
      multipart: true,
      fileName: 'cover.jpg',
    });
  });

  it('upload() turns an error response into an ApiError', async () => {
    server.use(
      http.post('/api/v1/recipes/9/cover', () =>
        HttpResponse.json(
          { status: 415, title: 'Unsupported Media Type', code: 'UNSUPPORTED_MEDIA_TYPE' },
          { status: 415, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    );

    await expect(api.upload('/recipes/9/cover', new FormData())).rejects.toMatchObject({
      status: 415,
      problem: { code: 'UNSUPPORTED_MEDIA_TYPE' },
    });
  });

  it('a header passed by the caller is not overwritten', async () => {
    server.use(http.get('/api/v1/whoami', echoHeaders));

    const { fetchJson } = await import('./client');
    await expect(fetchJson('/api/v1/whoami', { headers: { 'X-Dev-User-Id': '9' } })).resolves.toMatchObject({
      devUserId: '9',
    });
  });
});

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
