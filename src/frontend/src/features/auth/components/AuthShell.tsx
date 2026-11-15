import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';
import { Icon, type IconName } from '@/shared/ui/Icon';
import { strings } from '../strings.vi';

const TILES: ReadonlyArray<{ tone: string; icon: IconName }> = [
  { tone: 'wk-photo--warm', icon: 'bowl' },
  { tone: 'wk-photo--saffron', icon: 'egg' },
  { tone: 'wk-photo--cream', icon: 'fish' },
];

/** The two-column layout of the sign-in screens (W-AU01 … W-AU07): artwork on the left, the form card on the right. */
export function AuthShell({ children }: { children: ReactNode }) {
  return (
    <div className="wx-page wk-desktop" style={{ minHeight: '100vh' }}>
      <div className="wx-auth">
        <div className="wx-auth__art">
          <Link className="wk-logo" to="/" aria-label="WikiCook">
            Wiki<span>Cook</span>
          </Link>
          <div className="sx-stack" style={{ gap: 16 }}>
            <p className="wk-display">{strings.brand.tagline}</p>
            <p style={{ maxWidth: '40ch' }}>{strings.brand.blurb}</p>
          </div>
          <div className="wx-auth__tiles">
            {TILES.map(({ tone, icon }) => (
              <div key={tone} className={`wk-photo ${tone}`} role="img" aria-label={strings.brand.photoAlt}>
                <Icon name={icon} />
              </div>
            ))}
          </div>
        </div>
        <div className="wx-auth__form">
          <main className="wx-auth__card">{children}</main>
        </div>
      </div>
    </div>
  );
}
