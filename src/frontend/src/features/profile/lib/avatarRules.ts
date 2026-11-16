/** What the avatar picker accepts before anything is sent (011 FR-003, NFR-004); the server checks the bytes again. */

export const AVATAR_TYPES = ['image/jpeg', 'image/png', 'image/webp'] as const;
export const AVATAR_MAX_BYTES = 5 * 1024 * 1024;

export type AvatarProblem = 'type' | 'size';

export function checkAvatar(file: Pick<File, 'type' | 'size'>): AvatarProblem | null {
  if (!(AVATAR_TYPES as readonly string[]).includes(file.type)) return 'type';
  if (file.size > AVATAR_MAX_BYTES) return 'size';
  return null;
}
