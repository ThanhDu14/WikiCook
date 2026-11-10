import { strings } from '../strings.vi';

/** The disclaimer shown on every AI draft (FR-028); `short` for lists of suggestions. */
export function AiDisclaimer({ short = false }: { short?: boolean }) {
  return <span className="wk-small">{short ? strings.disclaimerShort : strings.disclaimer}</span>;
}
