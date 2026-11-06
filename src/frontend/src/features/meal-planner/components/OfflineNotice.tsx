import { vi } from '../strings.vi';
import { Icon } from './Icon';

/** Shown instead of the calendar without a connection (W-MP07 / MP-07, T094, FR-028). */
export function OfflineNotice({ onRetry }: { onRetry: () => void }) {
  return (
    <div className="wk-state" role="alert" style={{ marginBlock: 24 }}>
      <div className="wk-state__art">
        <Icon name="wifiOff" size={32} />
      </div>
      <h2 className="wk-h3">{vi.offlineTitle}</h2>
      <p>{vi.offlineHint}</p>
      <button type="button" className="wk-btn wk-btn--primary" onClick={onRetry}>
        <Icon name="refresh" />
        {vi.retry}
      </button>
    </div>
  );
}
