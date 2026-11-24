import type { ReactNode } from 'react';

/**
 * A block of the detail page owned by another spec (060 T038). It renders what the owner mounts and nothing while
 * the owner has not built it, so the page never shows a dead button. `owner` names the spec for the next reader.
 */
export function BlockSlot({ owner, children }: { owner: string; children?: ReactNode }) {
  if (!children) return null;
  return <div data-block-owner={owner}>{children}</div>;
}
