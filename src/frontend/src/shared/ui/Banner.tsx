import type { ReactNode } from 'react';
import { Icon, type IconName } from './Icon';

type Tone = 'info' | 'success' | 'warning' | 'danger';

const DEFAULT_ICON: Record<Tone, IconName> = { info: 'info', success: 'checkCircle', warning: 'info', danger: 'warning' };

/** `wk-banner` of the screens: an icon, a bold title and a line of text. `alert` makes it announce itself at once. */
export function Banner({
  tone,
  icon,
  title,
  children,
  alert,
  style,
}: {
  tone: Tone;
  icon?: IconName;
  title: ReactNode;
  children?: ReactNode;
  alert?: boolean;
  style?: React.CSSProperties;
}) {
  return (
    <div className={`wk-banner wk-banner--${tone}`} role={alert ? 'alert' : 'status'} style={style}>
      <Icon name={icon ?? DEFAULT_ICON[tone]} />
      <div>
        <strong>{title}</strong>
        {children}
      </div>
    </div>
  );
}
