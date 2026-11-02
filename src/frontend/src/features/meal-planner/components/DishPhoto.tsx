import { useState } from 'react';
import { vi } from '../strings.vi';
import { Icon } from './Icon';

/** Cover picture of a dish, or the `wk-photo` placeholder when there is none or it fails to load (edge case). */
export function DishPhoto({ url, name, size }: { url?: string | null; name: string; size?: number }) {
  const [failed, setFailed] = useState(false);
  const style = size ? { width: size, height: size } : undefined;
  if (url && !failed) {
    return (
      <div className="wk-photo" style={style}>
        <img src={url} alt={name} onError={() => setFailed(true)} className="h-full w-full object-cover" />
      </div>
    );
  }
  return (
    <div className="wk-photo wk-photo--cream" role="img" aria-label={vi.noPhoto} style={style}>
      <Icon name="dish" />
    </div>
  );
}
