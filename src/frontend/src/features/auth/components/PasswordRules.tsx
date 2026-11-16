import { Icon } from '@/shared/ui/Icon';
import { strings } from '../strings.vi';
import type { PasswordCheck } from '../lib/passwordRules';

const RULES: ReadonlyArray<{ key: 'length' | 'letter' | 'digit'; label: string }> = [
  { key: 'length', label: strings.register.ruleLength },
  { key: 'letter', label: strings.register.ruleLetter },
  { key: 'digit', label: strings.register.ruleDigit },
];

/** The checklist under the password field (US2-3): each rule shows whether the typed password meets it. */
export function PasswordRules({ check, touched }: { check: PasswordCheck; touched: boolean }) {
  return (
    <ul
      className="wk-small"
      aria-label={strings.register.rulesLabel}
      style={{ listStyle: 'none', margin: '-8px 0 0', padding: 0, display: 'grid', gap: 4 }}
    >
      {RULES.map(({ key, label }) => {
        const met = check[key];
        const color = met ? 'var(--color-success)' : touched ? 'var(--color-danger)' : 'var(--color-ink-muted)';
        return (
          <li key={key} data-met={met} style={{ display: 'flex', gap: 8, alignItems: 'center', color }}>
            <Icon name={met ? 'checkCircle' : 'close'} size={16} />
            <span>{label}</span>
            <span className="sr-only">{met ? ' – đạt' : ' – chưa đạt'}</span>
          </li>
        );
      })}
    </ul>
  );
}
