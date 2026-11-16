import { Link } from 'react-router-dom';
import { Icon } from '@/shared/ui/Icon';
import { strings } from '../strings.vi';

/** W-AU09: shown instead of a page the role may not open (US3-2); no content of that page was rendered. */
export function AccessDeniedPage() {
  return (
    <div className="wx-wrap wx-wrap--narrow wk-desktop">
      <div style={{ padding: '48px 0' }}>
        <div className="wk-state wk-state--error">
        <div className="wk-state__art">
          <Icon name="shield" size={32} />
        </div>
        <h2 className="wk-h3">{strings.accessDenied.title}</h2>
        <p className="wk-muted">{strings.accessDenied.body}</p>
        <Link className="wk-btn wk-btn--primary" to="/">
          {strings.accessDenied.toHome}
        </Link>
        </div>
      </div>
    </div>
  );
}
