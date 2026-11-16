import { http, HttpResponse } from 'msw';
import type { CurrentAccount } from '../types';

export const member: CurrentAccount = {
  id: 1,
  displayName: 'Thanh Du',
  email: 'thanhdu@gmail.com',
  role: 'MEMBER',
  emailVerified: true,
};

/** A problem+json response as the backend sends it (spec 010 contract). */
export const problem = (status: number, code: string, extra: Record<string, unknown> = {}) =>
  HttpResponse.json(
    { type: 'about:blank', title: code, status, code, ...extra },
    { status, headers: { 'Content-Type': 'application/problem+json' } },
  );

export const authResponse = (account: Partial<CurrentAccount> = {}, accessToken = 'jwt-test') =>
  HttpResponse.json({ accessToken, expiresIn: 900, account: { ...member, ...account } });

/** `POST /auth/refresh` that succeeds: the browser has a valid refresh cookie, so the app starts signed in. */
export const signedInHandler = (account: Partial<CurrentAccount> = {}) =>
  http.post('/api/v1/auth/refresh', () => authResponse(account));
