import { useRef, useState } from 'react';
import { Icon } from '@/shared/ui/Icon';
import { initials } from '@/features/auth/lib/initials';
import { AVATAR_TYPES, checkAvatar } from '../lib/avatarRules';
import { strings } from '../strings.vi';

interface Props {
  displayName: string;
  /** The saved avatar, if any. */
  currentUrl?: string | null;
  /** Picked file (or `null`) and whether to drop the saved one; the page sends them on save. */
  onChange: (change: { file: File | null; remove: boolean }) => void;
  /** An error from the server for the avatar, shown with the form's own checks. */
  serverError?: string | null;
}

/**
 * Chooses a profile picture (US2-3 … US2-5). A wrong type or a file over 5 MB is refused here and never sent
 * (SC-003). The preview is read with `FileReader` into a `data:` URL, because the page's CSP allows `data:` but not
 * `blob:` for images.
 */
export function AvatarPicker({ displayName, currentUrl, onChange, serverError }: Props) {
  const input = useRef<HTMLInputElement>(null);
  const [preview, setPreview] = useState<string | null>(null);
  const [removed, setRemoved] = useState(false);
  const [error, setError] = useState<string | null>(null);

  function pick(file: File | undefined) {
    if (!file) return;
    if (checkAvatar(file)) {
      setError(strings.edit.avatarRules);
      return;
    }
    setError(null);
    setRemoved(false);
    const reader = new FileReader();
    reader.onload = () => setPreview(typeof reader.result === 'string' ? reader.result : null);
    reader.readAsDataURL(file);
    onChange({ file, remove: false });
  }

  function remove() {
    setError(null);
    setPreview(null);
    setRemoved(true);
    if (input.current) input.current.value = '';
    onChange({ file: null, remove: true });
  }

  const shown = preview ?? (removed ? null : (currentUrl ?? null));
  const message = error ?? serverError ?? null;
  return (
    <div className="sx-row" style={{ gap: 24 }}>
      {shown ? (
        <img className="wk-avatar wk-avatar--xl" src={shown} alt={strings.edit.avatarAlt} style={{ objectFit: 'cover' }} />
      ) : (
        <span className="wk-avatar wk-avatar--xl" aria-hidden="true">
          {initials({ displayName })}
        </span>
      )}
      <div className="sx-stack" style={{ gap: 8 }}>
        <div className="sx-row">
          <input
            ref={input}
            type="file"
            accept={AVATAR_TYPES.join(',')}
            hidden
            aria-label={strings.edit.changePhoto}
            data-testid="avatar-input"
            onChange={(event) => pick(event.target.files?.[0])}
          />
          <button type="button" className="wk-btn wk-btn--secondary wk-btn--sm" onClick={() => input.current?.click()}>
            <Icon name="camera" />
            {strings.edit.changePhoto}
          </button>
          {shown ? (
            <button type="button" className="wk-btn wk-btn--ghost wk-btn--sm" onClick={remove}>
              {strings.edit.removePhoto}
            </button>
          ) : null}
        </div>
        <p className={message ? 'wk-error' : 'wk-helper'} role={message ? 'alert' : undefined}>
          {message ? <Icon name="warning" size={16} /> : null}
          {message ?? strings.edit.avatarRules}
        </p>
      </div>
    </div>
  );
}
