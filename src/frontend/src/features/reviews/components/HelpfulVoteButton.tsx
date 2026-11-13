import { strings } from '../strings.vi';
import { Icon } from './Icon';

interface HelpfulVoteButtonProps {
  count: number;
  voted: boolean;
  pending?: boolean;
  onToggle: () => void;
}

/** "Hữu ích (n)" toggle (T054, FR-022): wk-chip with a 44px touch target, pressed when the user has voted. */
export function HelpfulVoteButton({ count, voted, pending = false, onToggle }: HelpfulVoteButtonProps) {
  return (
    <button
      type="button"
      className="wk-chip"
      aria-pressed={voted}
      aria-label={strings.card.helpfulLabel(count, voted)}
      aria-busy={pending || undefined}
      onClick={onToggle}
      style={{ minHeight: 44, width: 'fit-content' }}
    >
      <Icon name="thumbsUp" className={voted ? 'is-on' : ''} />
      {strings.card.helpfulCount(count)}
    </button>
  );
}
