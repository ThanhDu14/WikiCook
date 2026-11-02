import { useState } from 'react';
import { vi } from '../strings.vi';
import { Icon } from './Icon';

/** Cover picture of a dish, or the `wk-photo` placeholder when there is none or it fails to load (edge case). */
export function DishPhoto({ url, name }: { url?: string | null; name: string }) {
  const [failed, setFailed] = useState(false);
  if (url && !failed) {
    return (
      <div className="wk-photo">
        <img src={url} alt={name} onError={() => setFailed(true)} className="h-full w-full object-cover" />
      </div>
    );
  }
  return (
    <div className="wk-photo wk-photo--cream" role="img" aria-label={vi.noPhoto}>
      <Icon name="dish" />
    </div>
  );
}
