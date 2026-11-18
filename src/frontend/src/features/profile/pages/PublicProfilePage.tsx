import { Link, useParams } from 'react-router-dom';
import { AccountAvatar } from '@/features/auth/components/AccountAvatar';
import { Icon } from '@/shared/ui/Icon';
import { ProfileSkeleton } from '../components/ProfileSkeleton';
import { usePublicProfile } from '../hooks/usePublicProfile';
import { strings } from '../strings.vi';

/**
 * The public page of a person (spec 011 US2-6, FR-005): picture, name, bio and how many recipes they published. Open
 * to guests, so it sits outside the member routes. It only knows `PublicProfile`, which carries no email and no
 * dietary data. The bio is plain text: React escapes it and links in it are not clickable.
 */
export function PublicProfilePage() {
  const { id } = useParams();
  const { state, reload } = usePublicProfile(id);

  if (state.status === 'loading') {
    return (
      <section className="sx-stack" style={{ maxWidth: 720, margin: '0 auto', width: '100%' }}>
        <ProfileSkeleton />
      </section>
    );
  }
  if (state.status === 'notFound') {
    return (
      <section className="wk-state wk-state--error" style={{ margin: '0 auto' }}>
        <div className="wk-state__art">
          <Icon name="warning" size={32} />
        </div>
        <h1 className="wk-h3">{strings.public.notFoundTitle}</h1>
        <p className="wk-muted">{strings.public.notFoundBody}</p>
        <Link className="wk-btn wk-btn--primary" to="/">
          {strings.public.toHome}
        </Link>
      </section>
    );
  }
  if (state.status === 'error') {
    return (
      <section className="wk-state wk-state--error" role="alert" style={{ margin: '0 auto' }}>
        <p>{strings.public.loadError}</p>
        <button type="button" className="wk-btn wk-btn--primary" onClick={reload}>
          {strings.retry}
        </button>
      </section>
    );
  }

  const { displayName, avatarUrl, bio, publishedRecipeCount } = state.data;
  return (
    <section className="sx-stack" style={{ maxWidth: 720, margin: '0 auto', width: '100%', gap: 24 }}>
      <div className="wx-panel" style={{ flexDirection: 'row', alignItems: 'center', gap: 24, flexWrap: 'wrap' }}>
        <AccountAvatar account={{ displayName, avatarUrl }} size="xl" />
        <div className="sx-grow sx-stack" style={{ gap: 4, minWidth: 0 }}>
          <h1 style={{ overflowWrap: 'anywhere' }}>{displayName}</h1>
          <p className="wk-muted">{strings.public.recipeCount(publishedRecipeCount)}</p>
        </div>
      </div>
      <div className="wx-panel">
        {bio ? (
          <p style={{ whiteSpace: 'pre-wrap', overflowWrap: 'anywhere' }}>{bio}</p>
        ) : (
          <p className="wk-muted">{strings.public.noBio}</p>
        )}
      </div>
    </section>
  );
}
