import { useEffect, useRef, useState } from 'react';
import { initials } from '../lib/format';
import { strings } from '../strings.vi';
import type { Cooksnap, Review } from '../types';
import { Icon } from './Icon';

interface CooksnapLightboxProps {
  review: Review;
  startIndex: number;
  onClose: () => void;
  /** Report the photo shown (not offered on the user's own review). */
  onReport?: (cooksnap: Cooksnap) => void;
}

const DARK_BUTTON = { background: '#211D19', color: '#F1E9DC', borderColor: '#3A332C' } as const;

/**
 * Full-size Cooksnap viewer of W-RV03 (T057, FR-023): open to guests too. ← → and the side buttons move (wrapping
 * around), the strip jumps to a photo, Esc or "Đóng" closes, and focus stays inside while it is open.
 */
export function CooksnapLightbox({ review, startIndex, onClose, onReport }: CooksnapLightboxProps) {
  const snaps = review.cooksnaps;
  const total = snaps.length;
  const [index, setIndex] = useState(() => Math.min(Math.max(startIndex, 0), Math.max(total - 1, 0)));
  const dialog = useRef<HTMLDivElement>(null);
  const onCloseRef = useRef(onClose);
  useEffect(() => {
    onCloseRef.current = onClose;
  });

  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    dialog.current?.focus();
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onCloseRef.current();
      else if (event.key === 'ArrowRight') setIndex((i) => (i + 1) % total);
      else if (event.key === 'ArrowLeft') setIndex((i) => (i - 1 + total) % total);
      else if (event.key === 'Tab' && dialog.current) {
        const focusable = Array.from(dialog.current.querySelectorAll<HTMLElement>('button'));
        if (focusable.length === 0) return;
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (event.shiftKey && (document.activeElement === first || document.activeElement === dialog.current)) {
          event.preventDefault();
          last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
          event.preventDefault();
          first.focus();
        }
      }
    };
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
      previous?.focus?.();
    };
  }, [total]);

  if (total === 0) return null;
  const snap = snaps[index];
  const author = review.author.isDeleted ? strings.card.deletedUser : review.author.displayName;

  return (
    <div
      ref={dialog}
      role="dialog"
      aria-modal="true"
      aria-label={strings.lightbox.label}
      tabIndex={-1}
      style={{
        position: 'fixed',
        inset: 0,
        background: 'rgba(23,20,17,.96)',
        color: '#F1E9DC',
        display: 'grid',
        gridTemplateRows: 'auto 1fr auto',
        zIndex: 50,
      }}
    >
      <div className="sx-between" style={{ padding: '20px 32px' }}>
        <span className="wk-byline">
          <span className={`wk-avatar wk-avatar--sm${review.author.isDeleted ? ' wk-avatar--deleted' : ''}`} aria-hidden="true">
            {review.author.isDeleted ? '?' : initials(review.author.displayName)}
          </span>
          <strong>{author}</strong>
          <span className="wk-small" style={{ opacity: 0.7 }} aria-live="polite">
            · {strings.lightbox.position(index + 1, total)}
          </span>
        </span>
        <span className="sx-row">
          {onReport && !review.isOwner && (
            <button
              className="wk-icon-btn"
              type="button"
              aria-label={strings.lightbox.report}
              style={DARK_BUTTON}
              onClick={() => onReport(snap)}
            >
              <Icon name="flag" />
            </button>
          )}
          <button className="wk-icon-btn" type="button" aria-label={strings.lightbox.close} style={DARK_BUTTON} onClick={onClose}>
            <Icon name="close" />
          </button>
        </span>
      </div>
      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'auto 1fr auto',
          // bounded row: without it the photo's max-height % has nothing to resolve against and it overflows down
          gridTemplateRows: 'minmax(0, 1fr)',
          alignItems: 'center',
          gap: 32,
          padding: '0 32px',
          minHeight: 0,
        }}
      >
        {total > 1 ? (
          <button
            className="wk-icon-btn"
            type="button"
            aria-label={strings.lightbox.previous}
            style={DARK_BUTTON}
            onClick={() => setIndex((i) => (i - 1 + total) % total)}
          >
            <Icon name="chevronLeft" />
          </button>
        ) : (
          <span />
        )}
        <div style={{ display: 'grid', gridTemplateRows: 'minmax(0, 1fr)', placeItems: 'center', height: '100%', minHeight: 0 }}>
          <img
            src={snap.url}
            alt={snap.caption || strings.form.previewAlt(index + 1)}
            style={{ maxHeight: 'min(640px, 100%)', maxWidth: '100%', borderRadius: 16, objectFit: 'contain' }}
          />
        </div>
        {total > 1 ? (
          <button
            className="wk-icon-btn"
            type="button"
            aria-label={strings.lightbox.next}
            style={DARK_BUTTON}
            onClick={() => setIndex((i) => (i + 1) % total)}
          >
            <Icon name="chevronRight" />
          </button>
        ) : (
          <span />
        )}
      </div>
      <div className="sx-row" style={{ justifyContent: 'center', gap: 12, padding: 20, flexWrap: 'wrap' }}>
        {total > 1 &&
          snaps.map((item, i) => (
            <button
              key={item.id}
              type="button"
              aria-label={strings.lightbox.thumb(i + 1)}
              aria-current={i === index ? 'true' : undefined}
              onClick={() => setIndex(i)}
              style={{
                width: 72,
                padding: 0,
                border: 0,
                background: 'none',
                borderRadius: 10,
                cursor: 'pointer',
                opacity: i === index ? 1 : 0.6,
                outline: i === index ? '2px solid #F1E9DC' : undefined,
                outlineOffset: 2,
              }}
            >
              <img
                src={item.thumbnailUrl}
                alt=""
                style={{ width: 72, aspectRatio: '1', objectFit: 'cover', borderRadius: 10, display: 'block' }}
              />
            </button>
          ))}
        <span className="wk-small" style={{ opacity: 0.7, marginLeft: 16 }}>
          {strings.lightbox.hint}
        </span>
      </div>
    </div>
  );
}
