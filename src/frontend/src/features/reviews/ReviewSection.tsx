import { useCallback, useContext, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { AuthContext } from '@/features/auth/authContext';
import { GUEST, getDevUserId } from '@/shared/api/session';
import { CooksnapLightbox } from './components/CooksnapLightbox';
import { DeleteConfirm } from './components/DeleteConfirm';
import { Icon } from './components/Icon';
import { RatingBreakdown } from './components/RatingBreakdown';
import { ReportDialog } from './components/ReportDialog';
import { ReviewForm } from './components/ReviewForm';
import { ReviewList } from './components/ReviewList';
import { SignInPrompt } from './components/SignInPrompt';
import { useReviewActions } from './hooks/useReviewActions';
import { useReviews } from './hooks/useReviews';
import { useReviewVote } from './hooks/useReviewVote';
import { errorMessage } from './lib/errors';
import { strings } from './strings.vi';
import type { ReportTarget, Review } from './types';

export interface ReviewSectionProps {
  recipeId: number;
  recipeTitle?: string;
  /** The author may read the reviews of their recipe but not rate it (US1-AS7, FR-003). */
  isRecipeAuthor?: boolean;
}

type Dialog =
  | { kind: 'write' }
  | { kind: 'edit'; review: Review }
  | { kind: 'delete'; review: Review }
  | { kind: 'report'; target: ReportTarget }
  | { kind: 'photo'; review: Review; index: number }
  | { kind: 'signIn'; title: string };

/** Signed in per spec 010; without an `AuthProvider` (unit tests) the dev identity decides. */
function useIsGuest(): boolean {
  const auth = useContext(AuthContext);
  return auth ? auth.status !== 'authenticated' : getDevUserId() === GUEST;
}

/**
 * Reviews & Cooksnap of one recipe (W-RV01..W-RV06, T060): rating summary with star filter and the write button in the
 * aside, the list in the main column, and every dialog (write/edit, delete, report, lightbox, sign-in) opened from
 * here. Guests can read and open photos; writing, voting and reporting ask them to sign in first (FR-017).
 */
export function ReviewSection({ recipeId, recipeTitle, isRecipeAuthor = false }: ReviewSectionProps) {
  const reviews = useReviews(recipeId);
  const actions = useReviewActions(recipeId);
  const isGuest = useIsGuest();
  const navigate = useNavigate();
  const location = useLocation();
  const [dialog, setDialog] = useState<Dialog | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [newReviewId, setNewReviewId] = useState<number | null>(null);

  const askSignIn = useCallback((title: string) => setDialog({ kind: 'signIn', title }), []);
  const vote = useReviewVote({
    onChange: reviews.patchReview,
    onUnauthenticated: () => askSignIn(strings.signIn.voteTitle),
  });

  const close = () => setDialog(null);
  const done = (message: string) => {
    setDialog(null);
    setNotice(message);
    reviews.reload();
  };
  const signInPath = `/login?redirect=${encodeURIComponent(location.pathname + location.search)}`;

  const { data, loading, error } = reviews;
  const hasOwnReview = data?.items.some((review) => review.isOwner) ?? false;
  const isEmpty = data !== null && data.ratingSummary.totalReviews === 0;

  const openWrite = () => (isGuest ? askSignIn(strings.signIn.writeTitle(recipeTitle)) : setDialog({ kind: 'write' }));
  const writeButton = (
    <button className="wk-btn wk-btn--warm wk-btn--block" type="button" onClick={openWrite}>
      <Icon name="write" />
      {strings.form.createTitle}
    </button>
  );
  const canWrite = !isRecipeAuthor && !hasOwnReview;

  let main: React.ReactNode;
  if (data === null && loading) {
    main = (
      <div className="sx-stack" style={{ gap: 16 }} aria-busy="true">
        <p className="wk-muted" role="status">
          {strings.section.loading}
        </p>
        {[0, 1].map((i) => (
          <div key={i} className="sx-stack" style={{ gap: 8, padding: '16px 0', borderBottom: '1px solid var(--color-border)' }}>
            <div className="wk-skel" style={{ height: 20, width: '38%' }} />
            <div className="wk-skel" style={{ height: 16, width: '90%' }} />
            <div className="wk-skel" style={{ height: 16, width: '65%' }} />
          </div>
        ))}
      </div>
    );
  } else if (data === null || (error && !loading)) {
    main = (
      <div className="wk-state" role="alert">
        <h2 className="wk-h3">{strings.section.loadError}</h2>
        <p>{errorMessage(error)}</p>
        <button className="wk-btn wk-btn--secondary" type="button" onClick={reviews.reload}>
          {strings.section.retry}
        </button>
      </div>
    );
  } else if (isEmpty) {
    main = (
      <div className="sx-stack" style={{ gap: 24 }}>
        <div className="wk-state">
          <h2 className="wk-h3">{strings.empty.title}</h2>
          <p>{strings.empty.body}</p>
          {canWrite && (
            <button className="wk-btn wk-btn--warm" type="button" onClick={openWrite}>
              {strings.form.createTitle}
            </button>
          )}
        </div>
        {isGuest && (
          <div className="wx-panel" style={{ flexDirection: 'row', alignItems: 'center', gap: 16 }}>
            <span className="sx-grow">
              <p className="wk-eyebrow">{strings.empty.guestEyebrow}</p>
              <p className="wk-small wk-muted">{strings.empty.guestBody}</p>
            </span>
            <button className="wk-btn wk-btn--secondary" type="button" onClick={() => navigate(signInPath)}>
              {strings.empty.guestCta}
            </button>
          </div>
        )}
      </div>
    );
  } else {
    main = (
      <div
        aria-busy={loading || undefined}
        style={{
          opacity: loading ? 0.6 : 1,
          transition: 'opacity var(--motion-fast) var(--ease-out)',
        }}
      >
        <ReviewList
          data={data}
          query={reviews.query}
          hasFilters={reviews.hasFilters}
          newReviewId={newReviewId}
          onSortChange={reviews.setSort}
          onToggleCooksnapOnly={reviews.toggleCooksnapOnly}
          onClearRating={() => reviews.setRating(null)}
          onClearFilters={reviews.clearFilters}
          onPageChange={reviews.setPage}
          actions={{
            onOpenPhoto: (review, index) => setDialog({ kind: 'photo', review, index }),
            onReport: (review) =>
              isGuest
                ? askSignIn(strings.signIn.reportTitle)
                : setDialog({ kind: 'report', target: { type: 'REVIEW', review } }),
            onEdit: (review) => setDialog({ kind: 'edit', review }),
            onDelete: (review) => setDialog({ kind: 'delete', review }),
            onToggleHelpful: (review) => (isGuest ? askSignIn(strings.signIn.voteTitle) : void vote.toggle(review)),
            isVotePending: vote.isPending,
          }}
        />
      </div>
    );
  }

  return (
    <section aria-label={strings.section.regionLabel} className="wx-split wx-split--left">
      <aside className="wx-aside sx-stack" style={{ gap: 20 }}>
        {data && (
          <RatingBreakdown
            summary={data.ratingSummary}
            selectedStar={reviews.query.rating}
            onSelectStar={reviews.setRating}
          />
        )}
        {isRecipeAuthor ? (
          <p className="wk-small wk-muted">{strings.section.authorNotice}</p>
        ) : (
          canWrite && data && !isEmpty && writeButton
        )}
      </aside>

      <div className="sx-stack" style={{ gap: 16, minWidth: 0 }}>
        {notice && (
          <p className="wk-banner wk-banner--success" role="status">
            {notice}
          </p>
        )}
        {vote.error && (
          <p className="wk-banner wk-banner--danger" role="alert">
            {vote.error}
          </p>
        )}
        {main}
      </div>

      {dialog?.kind === 'write' && (
        <ReviewForm
          recipeTitle={recipeTitle}
          onCancel={close}
          onSubmit={async (payload) => {
            const created = await actions.create(payload);
            setNewReviewId(created.id);
            done(strings.section.created);
          }}
        />
      )}
      {dialog?.kind === 'edit' && (
        <ReviewForm
          recipeTitle={recipeTitle}
          review={dialog.review}
          onCancel={close}
          onSubmit={async (payload) => {
            await actions.update(dialog.review.id, payload);
            done(strings.section.updated);
          }}
        />
      )}
      {dialog?.kind === 'delete' && (
        <DeleteConfirm
          onCancel={close}
          onConfirm={async () => {
            await actions.remove(dialog.review.id);
            done(strings.section.deleted);
          }}
        />
      )}
      {dialog?.kind === 'report' && (
        <ReportDialog
          target={dialog.target}
          onClose={close}
          onSubmit={async (payload) => {
            await actions.report(dialog.target, payload);
            setDialog(null);
            setNotice(strings.section.reported);
          }}
        />
      )}
      {dialog?.kind === 'photo' && (
        <CooksnapLightbox
          review={dialog.review}
          startIndex={dialog.index}
          onClose={close}
          onReport={(cooksnap) =>
            isGuest
              ? askSignIn(strings.signIn.reportTitle)
              : setDialog({ kind: 'report', target: { type: 'COOKSNAP', review: dialog.review, cooksnap } })
          }
        />
      )}
      {dialog?.kind === 'signIn' && <SignInPrompt title={dialog.title} onClose={close} />}
    </section>
  );
}
