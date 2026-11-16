import { api } from '@/shared/api/client';
import type { MyProfile, ProfileUpdate } from '../types';

/** `GET /me/profile`: the caller's own profile with account info (FR-001, FR-006). */
export const getMyProfile = () => api.get<MyProfile>('/me/profile');

/**
 * `POST /me/profile` (multipart): the `profile` part is JSON, the `avatar` part optional. The server checks the image
 * bytes, crops it square and re-encodes it, which also drops EXIF (FR-002 – FR-004).
 */
export function saveMyProfile(profile: ProfileUpdate, avatar: File | null): Promise<MyProfile> {
  const form = new FormData();
  form.append('profile', new Blob([JSON.stringify(profile)], { type: 'application/json' }), 'profile.json');
  if (avatar) form.append('avatar', avatar);
  return api.postForm<MyProfile>('/me/profile', form);
}
