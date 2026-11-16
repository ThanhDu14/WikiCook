/** Types of the authentication API, matching specs/010-user-authentication-roles/contracts/openapi.yaml. */

export type Role = 'MEMBER' | 'CONTRIBUTOR' | 'MODERATOR' | 'ADMINISTRATOR';

/** Weakest to strongest; `RequireRole` compares by this order (FR-021). */
export const ROLE_ORDER: readonly Role[] = ['MEMBER', 'CONTRIBUTOR', 'MODERATOR', 'ADMINISTRATOR'];

export interface RoleNotice {
  previousRole: Role;
  newRole: Role;
}

export interface CurrentAccount {
  id: number;
  displayName: string;
  email: string;
  role: Role;
  emailVerified: boolean;
  roleNotice?: RoleNotice | null;
}

export interface AuthResponse {
  accessToken: string;
  /** Seconds; 900. */
  expiresIn: number;
  account: CurrentAccount;
}

export interface RegisterRequest {
  displayName: string;
  email: string;
  password: string;
  confirmPassword: string;
}

export interface RegisterResponse {
  email: string;
  verificationEmailSent: boolean;
  verificationExpiresAt?: string | null;
  /** Only when `verificationEmailSent` is false: how long until the next link may be requested. */
  retryAfterSeconds?: number | null;
}

export interface LoginRequest {
  email: string;
  password: string;
  rememberMe: boolean;
}

export interface VerificationResult {
  status: 'VERIFIED' | 'ALREADY_VERIFIED';
}

export type ResendVerificationRequest = { email: string } | { token: string };

/** `loading` until the first refresh answers; `offline` and `expired` keep the page (FR-024, 030 FR-028, 040 FR-020). */
export type AuthStatus = 'loading' | 'guest' | 'authenticated' | 'offline' | 'expired';

/** Why an account cannot be used (FR-013): passed to `/account-status` through the router state or a URL fragment. */
export interface AccountRestriction {
  code: 'ACCOUNT_LOCKED' | 'ACCOUNT_SUSPENDED' | 'ACCOUNT_BANNED';
  reason?: string;
  /** Suspension end. */
  endsAt?: string | null;
  /** Lock end. */
  retryAt?: string | null;
  closureToken?: string | null;
}

/** One field error of a `VALIDATION_ERROR` problem (NFR-007). */
export interface FieldError {
  field: string;
  code: string;
}
