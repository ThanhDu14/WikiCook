import { useEffect, useRef, useState } from 'react';
import { initials, relativeTime } from '../lib/format';
import { strings } from '../strings.vi';
import type { Review } from '../types';
import { HelpfulVoteButton } from './HelpfulVoteButton';
import { Icon, Stars } from './Icon';

interface ReviewCardProps {
  review: Review;
  /** Just submitted in this visit: shows the "Mới" badge (US1-AS2). */
  isNew?: boolean;
  votePending?: boolean;
  onOpenPhoto: (index: number) => void;
  onReport: () => void;
  onEdit: () => void;
  onDelete: () => void;
  onToggleHelpful: () => void;
}

/**
 * One review of W-RV01 (T056): author, time, stars, comment, tip pill, Cooksnap thumbnails and the helpful vote.
 * The owner gets the "⋮" menu with Chỉnh sửa / Xóa (W-RV04); everyone else gets "Báo cáo".
 */
export function ReviewCard({
  review,
  isNew = false,
  votePending = false,
  onOpenPhoto,
  onReport,
  onEdit,
  onDelete,
  onToggleHelpful,
}: ReviewCardProps) {
  const [menuOpen, setMenuOpen] = useState(false);
  const menuButton = useRef<HTMLButtonElement>(null);
  const firstItem = useRef<HTMLButtonElement>(null);
  const { author } = review;
  const name = review.isOwner ? strings.card.you : author.isDeleted ? strings.card.deletedUser : author.displayName;
  // the server stamps createdAt and updatedAt microseconds apart on create, so only a real later save counts
  const edited = Date.parse(review.updatedAt) - Date.parse(review.createdAt) > 1000;

  useEffect(() => {
    if (!menuOpen) return;
    firstItem.current?.focus();
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setMenuOpen(false);
        menuButton.current?.focus();
      }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [menuOpen]);

  const choose = (action: () => void) => {
    setMenuOpen(false);
    action();
  };

  return (
    <article
      className="sx-stack"
      style={{ gap: 8, padding: '20px 0', borderBottom: '1px solid var(--color-border)', position: 'relative' }}
    >
      <div className="sx-between">
        <span className="wk-byline">
          {author.isDeleted ? (
            <span className="wk-avatar wk-avatar--sm wk-avatar--deleted">?</span>
          ) : author.avatarUrl ? (
            <img className="wk-avatar wk-avatar--sm" src={author.avatarUrl} alt="" />
          ) : (
            <span className="wk-avatar wk-avatar--sm" aria-hidden="true">
              {initials(author.displayName)}
            </span>
          )}
          <strong>{name}</strong>
          <span className="wk-caption wk-muted">
            · {relativeTime(review.createdAt)}
            {edited && ` · ${strings.card.edited}`}
          </span>
          {isNew && <span className="wk-badge wk-badge--ai">{strings.card.newBadge}</span>}
        </span>
        {review.isOwner ? (
          <button
            ref={menuButton}
            className="wk-icon-btn wk-icon-btn--plain"
            type="button"
            aria-label={strings.card.options}
            aria-haspopup="menu"
            aria-expanded={menuOpen}
            onClick={() => setMenuOpen((open) => !open)}
          >
            <Icon name="more" />
          </button>
        ) : (
          <button className="wk-icon-btn wk-icon-btn--plain" type="button" aria-label={strings.card.report} onClick={onReport}>
            <Icon name="flag" />
          </button>
        )}
      </div>
      <Stars filled={review.rating} label={strings.card.stars(review.rating)} />
      {review.comment && <p style={{ whiteSpace: 'pre-line', overflowWrap: 'anywhere' }}>{review.comment}</p>}
      {review.substitutionTip && (
        <p
          className="wk-small"
          style={{ background: 'var(--color-saffron-tint)', padding: '8px 12px', borderRadius: 12, overflowWrap: 'anywhere' }}
        >
          <strong>{strings.card.tip}</strong> {review.substitutionTip}
        </p>
      )}
      {review.cooksnaps.length > 0 && (
        <div className="wk-snaps" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(96px, 120px))' }}>
          {review.cooksnaps.map((snap, index) => (
            <button
              key={snap.id}
              type="button"
              className="wk-photo"
              aria-label={strings.card.openPhoto(index + 1, review.cooksnaps.length)}
              onClick={() => onOpenPhoto(index)}
              style={{ padding: 0, border: 0, cursor: 'zoom-in' }}
            >
              <img
                src={snap.thumbnailUrl}
                alt={snap.caption ?? ''}
                loading="lazy"
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
              />
            </button>
          ))}
        </div>
      )}
      {!review.isOwner && (
        <HelpfulVoteButton
          count={review.helpfulCount}
          voted={review.hasVotedHelpful}
          pending={votePending}
          onToggle={onToggleHelpful}
        />
      )}
      {menuOpen && (
        <div className="wx-modal" role="menu" style={{ position: 'absolute', top: 56, right: 0, width: 220, padding: 8, zIndex: 5 }}>
          <div className="sx-list">
            <button ref={firstItem} type="button" role="menuitem" onClick={() => choose(onEdit)}>
              <Icon name="pencil" />
              {strings.card.edit}
            </button>
            <button type="button" role="menuitem" style={{ color: 'var(--color-danger)' }} onClick={() => choose(onDelete)}>
              <Icon name="trash" />
              {strings.card.delete}
            </button>
          </div>
        </div>
      )}
    </article>
  );
}
