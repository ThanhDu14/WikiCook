import { strings } from '../strings.vi';

/** The loading state of the profile pages (FR-039): grey blocks where the form will be, announced once. */
export function ProfileSkeleton() {
  return (
    <div role="status" aria-label={strings.loading} aria-busy="true" className="sx-stack" style={{ gap: 16 }}>
      <div className="wk-skel" style={{ height: 32, width: '40%' }} />
      <div className="wk-skel" style={{ height: 120 }} />
      <div className="wk-skel" style={{ height: 120 }} />
    </div>
  );
}
