import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { initials } from '@/features/auth/lib/initials';
import { Icon, type IconName } from '@/shared/ui/Icon';
import { getDietaryProfile } from '../api/dietaryApi';
import { AccountLayout } from '../components/AccountLayout';
import { ProfileSkeleton } from '../components/ProfileSkeleton';
import { useMyProfile } from '../hooks/useMyProfile';
import { ROLE_LABELS, strings } from '../strings.vi';
import type { DietaryProfile } from '../types';

const panel = { flexDirection: 'row', alignItems: 'center', gap: 16, textDecoration: 'none', color: 'var(--color-ink)' } as const;
const art = { width: 48, height: 48 } as const;

function Card({ icon, title, hint, to }: { icon: IconName; title: string; hint: string; to?: string }) {
  const body = (
    <>
      <span className="wk-state__art" style={art}>
        <Icon name={icon} />
      </span>
      <span className="sx-grow">
        <strong>{title}</strong>
        <br />
        <span className="wk-small wk-muted">{hint}</span>
      </span>
      <Icon name="chevronRight" />
    </>
  );
  return to ? (
    <Link className="wx-panel" to={to} style={panel}>
      {body}
    </Link>
  ) : (
    <span className="wx-panel cursor-default" aria-disabled="true" title="Sắp có" style={panel}>
      {body}
    </span>
  );
}

/** "Dị ứng đậu phộng · 4 người", or "Chưa thiết lập" for a profile never saved. */
function dietarySummary(profile: DietaryProfile | null): string {
  if (!profile || profile.empty) return profile ? strings.me.dietaryNotSet : strings.me.dietaryCardHint;
  const parts: string[] = [];
  if (profile.allergens.length > 0) {
    parts.push(strings.me.dietaryAllergy(profile.allergens.map((a) => a.nameVi.toLocaleLowerCase('vi-VN')).join(', ')));
  }
  if (profile.householdSize) parts.push(strings.me.dietaryHousehold(profile.householdSize));
  return parts.length > 0 ? parts.join(' · ') : strings.me.dietaryCardHint;
}

/** W-PF01 Tổng quan tài khoản (FR-001): who you are, and the doors to the profile pages. */
export function MyAccountPage() {
  const { state, reload } = useMyProfile();
  const [dietary, setDietary] = useState<DietaryProfile | null>(null);

  useEffect(() => {
    let cancelled = false;
    getDietaryProfile()
      .then((profile) => !cancelled && setDietary(profile))
      .catch(() => undefined); // the card keeps its generic line
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <AccountLayout>
      {state.status === 'loading' ? <ProfileSkeleton /> : null}
      {state.status === 'error' ? (
        <div className="wk-state wk-state--error" role="alert">
          <p>{strings.me.loadError}</p>
          <button type="button" className="wk-btn wk-btn--primary" onClick={reload}>
            {strings.retry}
          </button>
        </div>
      ) : null}
      {state.status === 'ready' ? (
        <>
          <div className="wx-panel" style={{ flexDirection: 'row', alignItems: 'center', gap: 24 }}>
            {state.data.avatarUrl ? (
              <img className="wk-avatar wk-avatar--xl" src={state.data.avatarUrl} alt="" style={{ objectFit: 'cover' }} />
            ) : (
              <span className="wk-avatar wk-avatar--xl" aria-hidden="true">
                {initials(state.data)}
              </span>
            )}
            <div className="sx-grow sx-stack" style={{ gap: 4 }}>
              <h1>{state.data.displayName}</h1>
              <p className="wk-muted">
                {state.data.account.email} · {ROLE_LABELS[state.data.account.role]}
                {state.data.account.emailVerified ? '' : ` · ${strings.me.unverified}`}
              </p>
            </div>
            <span className="wk-btn wk-btn--secondary wk-btn--sm cursor-default" aria-disabled="true" title="Sắp có">
              <Icon name="eye" />
              {strings.me.viewPublic}
            </span>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, minmax(0, 1fr))', gap: 16 }}>
            <Card icon="salad" title={strings.me.dietaryCard} hint={dietarySummary(dietary)} to="/profile/dietary" />
            <Card icon="chef" title={strings.me.myRecipesCard} hint={strings.me.myRecipesHint} />
            <Card icon="history" title={strings.me.historyCard} hint={strings.me.historyCardHint} to="/shopping-list/history" />
            <Card icon="eye" title={strings.me.publicCard} hint={strings.me.publicCardHint} to="/profile/edit" />
            <Card icon="lock" title={strings.me.securityCard} hint={strings.me.securityHint} />
          </div>
        </>
      ) : null}
    </AccountLayout>
  );
}
