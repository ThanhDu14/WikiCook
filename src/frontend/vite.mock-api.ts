import type { IncomingMessage, ServerResponse } from 'node:http';
import type { Plugin } from 'vite';

/**
 * Fake backend for looking at the sign-in and profile screens without the Spring app: `npm run dev:mock`.
 * It only exists in the dev server and only in `--mode mock`; the real build and `npm run dev` never load it.
 * State lives in memory and resets when the dev server restarts.
 *
 * Handy inputs: any e-mail and password sign in; the password `sai` is refused (INVALID_CREDENTIALS); the e-mail
 * `trung@gmail.com` is "already registered"; the verify link `#token=het-han` is expired; `#token=ok` verifies.
 */
export function mockApi(): Plugin {
  let account: { id: number; displayName: string; email: string; role: string; emailVerified: boolean } | null = null;
  let profile = { displayName: 'Thanh Du', bio: 'Nấu cơm nhà cho 4 người mỗi tối.', avatarUrl: null as string | null, version: 1 };
  let dietary = {
    allergens: [] as { id: number; code: string; nameVi: string }[],
    excludedIngredients: [] as { id: number; name: string }[],
    dietType: 'NONE',
    preferredCuisines: [] as { id: number; name: string }[],
    householdSize: null as number | null,
    maxCookTimeMinutes: null as number | null,
    empty: true,
    version: 0,
    notices: [] as unknown[],
  };
  const allergens = [
    { id: 1, code: 'PEANUT', nameVi: 'Đậu phộng' },
    { id: 2, code: 'SHELLFISH', nameVi: 'Tôm, cua' },
    { id: 3, code: 'MILK', nameVi: 'Sữa' },
    { id: 4, code: 'GLUTEN', nameVi: 'Gluten' },
    { id: 5, code: 'EGG', nameVi: 'Trứng' },
    { id: 6, code: 'SOY', nameVi: 'Đậu nành' },
  ];
  const cuisines = [
    { id: 10, name: 'Việt Nam' },
    { id: 11, name: 'Nhật' },
    { id: 12, name: 'Hàn' },
    { id: 13, name: 'Thái' },
  ];
  const ingredients = ['Ngò rí', 'Ngò gai', 'Rau răm', 'Hành lá', 'Tỏi', 'Sả', 'Ớt', 'Đậu phụ'].map((name, index) => ({ id: 100 + index, name }));
  const strip = (text: string) => text.normalize('NFD').replace(/[̀-ͯ]/g, '').replace(/đ/g, 'd').toLowerCase();

  const read = (req: IncomingMessage) =>
    new Promise<string>((resolve) => {
      const chunks: Buffer[] = [];
      req.on('data', (chunk: Buffer) => chunks.push(chunk));
      req.on('end', () => resolve(Buffer.concat(chunks).toString('utf8')));
    });
  const send = (res: ServerResponse, status: number, body?: unknown) => {
    res.statusCode = status;
    if (body !== undefined) {
      res.setHeader('Content-Type', status >= 400 ? 'application/problem+json' : 'application/json');
      res.end(JSON.stringify(body));
    } else res.end();
  };
  const problem = (res: ServerResponse, status: number, code: string, extra: object = {}) => send(res, status, { status, title: code, code, ...extra });
  const session = () => ({ accessToken: 'mock-token', expiresIn: 900, account });

  return {
    name: 'wikicook-mock-api',
    apply: 'serve',
    configureServer(server) {
      server.middlewares.use('/api/v1', async (req, res, next) => {
        const url = new URL(req.url ?? '/', 'http://mock');
        const path = url.pathname;
        const method = req.method ?? 'GET';
        const raw = method === 'GET' ? '' : await read(req);
        const json = (): Record<string, unknown> => {
          try {
            return JSON.parse(raw || '{}') as Record<string, unknown>;
          } catch {
            return {};
          }
        };
        const needAccount = () => {
          if (!account) problem(res, 401, 'UNAUTHENTICATED');
          return account;
        };

        if (path === '/auth/refresh' && method === 'POST') return account ? send(res, 200, session()) : problem(res, 401, 'UNAUTHENTICATED');
        if (path === '/auth/login' && method === 'POST') {
          const { email, password } = json() as { email?: string; password?: string };
          if (!email || password === 'sai') return problem(res, 401, 'INVALID_CREDENTIALS');
          account = { id: 1, displayName: profile.displayName, email, role: 'MEMBER', emailVerified: true };
          return send(res, 200, session());
        }
        if (path === '/auth/logout' && method === 'POST') {
          account = null;
          return send(res, 204);
        }
        if (path === '/auth/register' && method === 'POST') {
          const { email } = json() as { email?: string };
          if (email === 'trung@gmail.com') return problem(res, 409, 'EMAIL_TAKEN');
          return send(res, 201, { email, verificationEmailSent: true, verificationExpiresAt: new Date(Date.now() + 864e5).toISOString() });
        }
        if (path === '/auth/me') return needAccount() && send(res, 200, account);
        if (path === '/auth/verification/resend') return send(res, 202);
        if (path === '/auth/verification/confirm') {
          return (json() as { token?: string }).token === 'het-han' ? problem(res, 410, 'TOKEN_EXPIRED') : send(res, 200, { status: 'VERIFIED' });
        }
        if (path === '/auth/password/forgot') return send(res, 202);
        if (path === '/auth/password/reset/validate') {
          return (json() as { token?: string }).token === 'het-han'
            ? problem(res, 410, 'TOKEN_EXPIRED')
            : send(res, 200, { email: 'duy.nguyen@gmail.com', expiresAt: new Date(Date.now() + 18e5).toISOString() });
        }
        if (path === '/auth/password/reset') return send(res, 204);

        if (path === '/me/profile') {
          if (!needAccount()) return;
          if (method === 'POST') {
            // multipart: the profile part is JSON; the mock only reads the name and bio out of it
            const name = /"displayName"\s*:\s*"((?:[^"\\]|\\.)*)"/.exec(raw);
            const bio = /"bio"\s*:\s*(null|"((?:[^"\\]|\\.)*)")/.exec(raw);
            if (name) profile.displayName = JSON.parse(`"${name[1]}"`) as string;
            if (bio) profile.bio = bio[2] === undefined ? null! : (JSON.parse(`"${bio[2]}"`) as string);
            profile = { ...profile, version: profile.version + 1 };
            account = { ...account!, displayName: profile.displayName };
          }
          return send(res, 200, {
            id: 1,
            ...profile,
            account: { email: account!.email, role: 'MEMBER', status: 'ACTIVE', emailVerified: true, hasPassword: true, googleLinked: false, createdAt: '2026-10-01T00:00:00Z' },
          });
        }
        if (path === '/me/dietary-options') return needAccount() && send(res, 200, { allergens, dietTypes: ['NONE', 'VEGETARIAN', 'VEGAN', 'PESCATARIAN'], cuisines });
        if (path === '/me/dietary-profile') {
          if (!needAccount()) return;
          if (method === 'PUT') {
            const body = json() as { allergenIds: number[]; excludedIngredientIds: number[]; dietType: string; preferredCuisineIds: number[]; householdSize: number | null; maxCookTimeMinutes: number | null };
            dietary = {
              ...dietary,
              allergens: allergens.filter((a) => body.allergenIds.includes(a.id)),
              excludedIngredients: ingredients.filter((i) => body.excludedIngredientIds.includes(i.id)),
              dietType: body.dietType,
              preferredCuisines: cuisines.filter((c) => body.preferredCuisineIds.includes(c.id)),
              householdSize: body.householdSize,
              maxCookTimeMinutes: body.maxCookTimeMinutes,
              empty: false,
              version: dietary.version + 1,
            };
          }
          return send(res, 200, dietary);
        }
        if (path === '/ingredients/suggestions') {
          const q = strip(url.searchParams.get('q') ?? '');
          return send(res, 200, q.length < 2 ? [] : ingredients.filter((i) => strip(i.name).includes(q)).slice(0, 10));
        }
        next();
      });
    },
  };
}
