import { ICON_PATHS, type IconName } from './iconPaths';

export function Icon({ name, className = '' }: { name: IconName; className?: string }) {
  return (
    <svg className={`wk-icon ${className}`.trim()} viewBox="0 0 24 24" aria-hidden="true">
      {ICON_PATHS[name]}
    </svg>
  );
}
