import type { Page, Route } from '@playwright/test';
import { NOW } from './fakeApi';

/** One pixel, served for every avatar request so no image fails to load. */
const PIXEL = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==', 'base64');
export const AVATAR_URL = '/api/v1/media/avatars/e2e-avatar.png';

export interface Account {
  id: number;
  displayName: string;
  email: string;
  role: string;
  emailVerified: boolean;
  avatarUrl?: string | null;
}

/** What `POST /auth/login` answers: it works, or the account is suspended or banned (spec 010 FR-013). */
export type LoginOutcome = 'ok' | 'suspended' | 'banned';

export const SUSPENDED = { reason: 'Đăng quảng cáo nhiều lần', endsAt: '2026-10-30T00:00:00Z' };
export const BANNED = { reason: 'Vi phạm nhiều lần' };

const MEMBER: Account = { id: 1, displayName: 'Thanh Du', email: 'thanhdu@gmail.com', role: 'MEMBER', emailVerified: true };

const ALLERGENS = [
  { id: 1, code: 'PEANUT', nameVi: 'Đậu phộng' },
  { id: 2, code: 'SHELLFISH', nameVi: 'Tôm, cua' },
  { id: 3, code: 'MILK', nameVi: 'Sữa' },
];
const CUISINES = [
  { id: 10, name: 'Việt Nam' },
  { id: 11, name: 'Nhật' },
];

/** What a guest may read of a person (`GET /profiles/{id}`); the email and the diet are never in it. */
export const COOK = { id: 2, displayName: 'Minh Anh', avatarUrl: null, bio: 'Thích món chay và bánh ngọt.', publishedRecipeCount: 12 };

interface Options {
  /** Signed in at start-up (the refresh cookie is valid). Default: a guest. */
  signedIn?: boolean;
  login?: LoginOutcome;
}

/**
 * In-memory backend of the sign-in and profile screens (specs 010 and 011) for one page, in the style of `FakeApi`:
 * installed with `page.route`, so no server or database is needed. `calls` lists "METHOD path" of what a test did
 * (the start-up refresh and `/auth/me` are not counted); `saved` holds what the profile form sent.
 */
export class FakeAuthApi {
  readonly calls: string[] = [];
  readonly saved: Array<{ displayName?: string; bio?: string | null; removeAvatar?: boolean; avatar: boolean }> = [];
  dietarySaved: Record<string, unknown> | null = null;
  account: Account | null;
  private readonly login: LoginOutcome;
  private profile = { displayName: MEMBER.displayName, bio: 'Nấu cơm nhà cho 4 người mỗi tối.' as string | null, avatarUrl: null as string | null, version: 1 };
  private dietary = {
    allergens: [] as typeof ALLERGENS,
    excludedIngredients: [] as Array<{ id: number; name: string }>,
    dietType: 'NONE',
    preferredCuisines: [] as typeof CUISINES,
    householdSize: null as number | null,
    maxCookTimeMinutes: null as number | null,
    empty: true,
    version: 0,
    notices: [] as unknown[],
  };

  constructor(options: Options = {}) {
    this.account = options.signedIn ? { ...MEMBER } : null;
    this.login = options.login ?? 'ok';
  }

  async install(page: Page) {
    await page.clock.setFixedTime(NOW);
    await page.route('**/actuator/health', (route) => route.fulfill({ json: { status: 'UP' } }));
    await page.route('**/api/v1/**', (route) => this.handle(route));
    await page.route('**/api/v1/media/avatars/*', (route) => route.fulfill({ body: PIXEL, contentType: 'image/png' }));
  }

  private session() {
    return { accessToken: 'e2e-token', expiresIn: 900, account: this.account };
  }

  private async handle(route: Route) {
    const request = route.request();
    const path = new URL(request.url()).pathname.replace(/^\/api\/v1/, '');
    const method = request.method();
    const problem = (status: number, code: string, extra: Record<string, unknown> = {}) =>
      route.fulfill({ status, contentType: 'application/problem+json', json: { status, title: code, code, ...extra } });

    if (method === 'POST' && path === '/auth/refresh') return this.account ? route.fulfill({ json: this.session() }) : problem(401, 'UNAUTHENTICATED');
    if (method === 'GET' && path === '/auth/me') return this.account ? route.fulfill({ json: this.account }) : problem(401, 'UNAUTHENTICATED');
    this.calls.push(`${method} ${path}`);

    if (method === 'POST' && path === '/auth/login') {
      if (this.login === 'suspended') return problem(403, 'ACCOUNT_SUSPENDED', SUSPENDED);
      if (this.login === 'banned') return problem(403, 'ACCOUNT_BANNED', BANNED);
      this.account = { ...MEMBER, displayName: this.profile.displayName, avatarUrl: this.profile.avatarUrl };
      return route.fulfill({ json: this.session() });
    }
    if (method === 'POST' && path === '/auth/logout') {
      this.account = null;
      return route.fulfill({ status: 204 });
    }
    if (method === 'POST' && path === '/auth/register') {
      const body = request.postDataJSON() as { email: string };
      if (body.email === 'trung@gmail.com') return problem(409, 'EMAIL_TAKEN');
      return route.fulfill({ status: 201, json: { email: body.email, verificationEmailSent: true, verificationExpiresAt: '2026-10-22T09:00:00Z' } });
    }

    const publicId = /^\/profiles\/(\d+)$/.exec(path)?.[1];
    if (method === 'GET' && publicId) {
      if (publicId === '1') return route.fulfill({ json: { id: 1, ...this.profile, publishedRecipeCount: 3 } });
      return publicId === String(COOK.id) ? route.fulfill({ json: COOK }) : problem(404, 'USER_NOT_FOUND');
    }

    if (!this.account) return problem(401, 'UNAUTHENTICATED');
    if (path === '/me/profile') {
      if (method === 'POST') this.saveProfile(request.postData() ?? '');
      return route.fulfill({
        json: {
          id: 1,
          ...this.profile,
          account: { email: MEMBER.email, role: 'MEMBER', status: 'ACTIVE', emailVerified: true, hasPassword: true, googleLinked: false, createdAt: '2026-10-01T00:00:00Z' },
        },
      });
    }
    if (path === '/me/dietary-options') return route.fulfill({ json: { allergens: ALLERGENS, dietTypes: ['NONE', 'VEGETARIAN', 'VEGAN', 'PESCATARIAN'], cuisines: CUISINES } });
    if (path === '/me/dietary-profile') {
      if (method === 'PUT') {
        const body = request.postDataJSON() as { allergenIds: number[]; dietType: string; preferredCuisineIds: number[]; householdSize: number | null; maxCookTimeMinutes: number | null };
        this.dietarySaved = body;
        this.dietary = {
          ...this.dietary,
          allergens: ALLERGENS.filter((a) => body.allergenIds.includes(a.id)),
          dietType: body.dietType,
          preferredCuisines: CUISINES.filter((c) => body.preferredCuisineIds.includes(c.id)),
          householdSize: body.householdSize,
          maxCookTimeMinutes: body.maxCookTimeMinutes,
          empty: false,
          version: this.dietary.version + 1,
        };
      }
      return route.fulfill({ json: this.dietary });
    }
    if (path === '/ingredients/suggestions') return route.fulfill({ json: [] });
    return problem(404, 'NOT_FOUND');
  }

  /** The `profile` part of the multipart request is JSON; the `avatar` part is present when a picture was sent. */
  private saveProfile(raw: string) {
    const json = /\{[^\n]*"displayName"[^\n]*\}/.exec(raw)?.[0];
    const sent = json ? (JSON.parse(json) as { displayName: string; bio: string | null; removeAvatar: boolean }) : null;
    const avatar = /name="avatar"/.test(raw);
    this.saved.push({ ...sent!, avatar });
    if (!sent) return;
    this.profile = { displayName: sent.displayName, bio: sent.bio, avatarUrl: avatar ? AVATAR_URL : sent.removeAvatar ? null : this.profile.avatarUrl, version: this.profile.version + 1 };
    this.account = { ...this.account!, displayName: this.profile.displayName, avatarUrl: this.profile.avatarUrl };
  }
}
