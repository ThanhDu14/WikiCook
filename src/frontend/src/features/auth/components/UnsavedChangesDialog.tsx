import { useEffect, useRef } from 'react';
import type { Blocker } from 'react-router-dom';
import { strings } from '../strings.vi';

function Blocked({ blocker }: { blocker: Extract<Blocker, { state: 'blocked' }> }) {
  const stay = useRef<HTMLButtonElement>(null);
  const resetRef = useRef(blocker.reset);
  useEffect(() => {
    resetRef.current = blocker.reset;
  });
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null;
    stay.current?.focus();
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') resetRef.current();
    };
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('keydown', onKey);
      previous?.focus?.();
    };
  }, []);
  return (
    <div className="wk-scrim" role="presentation" style={{ position: 'fixed', zIndex: 60 }}>
      <div className="wk-dialog" role="alertdialog" aria-modal="true" aria-labelledby="leave-title" aria-describedby="leave-body">
        <h2 className="wk-h3" id="leave-title">
          {strings.leave.title}
        </h2>
        <p id="leave-body">{strings.leave.body}</p>
        <div className="wk-dialog__actions">
          <button ref={stay} type="button" className="wk-btn wk-btn--secondary" onClick={() => blocker.reset()}>
            {strings.leave.stay}
          </button>
          <button type="button" className="wk-btn wk-btn--danger" onClick={() => blocker.proceed()}>
            {strings.leave.leave}
          </button>
        </div>
      </div>
    </div>
  );
}

/** The confirmation of a blocked navigation: "Ở lại" or "Rời đi". */
export function UnsavedChangesDialog({ blocker }: { blocker: Blocker }) {
  return blocker.state === 'blocked' ? <Blocked blocker={blocker} /> : null;
}
