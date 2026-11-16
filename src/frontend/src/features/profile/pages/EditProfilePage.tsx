import { useState, type FormEvent } from 'react';
import { ApiError } from '@/shared/api/client';
import { Banner } from '@/shared/ui/Banner';
import { TextField } from '@/shared/ui/TextField';
import { UnsavedChangesDialog } from '@/features/auth/components/UnsavedChangesDialog';
import { useUnsavedChangesBlocker } from '@/features/auth/hooks/useUnsavedChangesBlocker';
import { problemCode } from '@/features/auth/lib/problem';
import { useAuth } from '@/features/auth/useAuth';
import { saveMyProfile } from '../api/profileApi';
import { AccountLayout } from '../components/AccountLayout';
import { AvatarPicker } from '../components/AvatarPicker';
import { ProfileSkeleton } from '../components/ProfileSkeleton';
import { useMyProfile } from '../hooks/useMyProfile';
import { codePointLength, stripped } from '../lib/codePoints';
import { strings } from '../strings.vi';
import type { MyProfile } from '../types';

const NAME_MAX = 50;
const BIO_MAX = 300;

function EditProfileForm({ profile, onReload }: { profile: MyProfile; onReload: () => void }) {
  const { updateAccount } = useAuth();
  const [saved, setSaved] = useState(profile);
  const [displayName, setDisplayName] = useState(profile.displayName);
  const [bio, setBio] = useState(profile.bio ?? '');
  const [avatar, setAvatar] = useState<{ file: File | null; remove: boolean }>({ file: null, remove: false });
  const [errors, setErrors] = useState<{ displayName?: string; bio?: string }>({});
  const [avatarError, setAvatarError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [conflict, setConflict] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const dirty =
    stripped(displayName) !== saved.displayName || stripped(bio) !== (saved.bio ?? '') || avatar.file !== null || avatar.remove;
  const blocker = useUnsavedChangesBlocker(dirty && !saving);

  async function submit(event: FormEvent) {
    event.preventDefault();
    if (saving) return;

    const found: typeof errors = {};
    const name = stripped(displayName);
    if (name === '') found.displayName = strings.edit.nameRequired;
    else if (codePointLength(name) > NAME_MAX) found.displayName = strings.edit.nameTooLong;
    if (codePointLength(stripped(bio)) > BIO_MAX) found.bio = strings.edit.bioTooLong;
    setErrors(found);
    setFormError(null);
    setAvatarError(null);
    setNotice(null);
    if (Object.keys(found).length > 0) return;

    setSaving(true);
    try {
      const next = await saveMyProfile(
        { displayName: name, bio: stripped(bio) === '' ? null : stripped(bio), removeAvatar: avatar.remove, version: saved.version },
        avatar.file,
      );
      setSaved(next);
      setDisplayName(next.displayName);
      setBio(next.bio ?? '');
      setAvatar({ file: null, remove: false });
      updateAccount({ displayName: next.displayName });
      setNotice(strings.edit.saved);
    } catch (error) {
      const code = problemCode(error);
      if (code === 'PROFILE_CHANGED') {
        setConflict(true);
      } else if (code && code in strings.errors.avatar) {
        setAvatarError(strings.errors.avatar[code]);
      } else if (code === 'VALIDATION_ERROR' && error instanceof ApiError) {
        setErrors({ displayName: strings.errors.required });
      } else if (error instanceof ApiError) {
        setFormError(strings.errors.generic);
      } else {
        setFormError(strings.errors.network);
      }
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={submit} noValidate className="sx-stack" style={{ gap: 24 }}>
      <UnsavedChangesDialog blocker={blocker} />
      <div className="wx-head">
        <div>
          <h1>{strings.edit.title}</h1>
        </div>
        <div className="sx-row">
          <button type="submit" className="wk-btn wk-btn--primary" disabled={saving || !dirty}>
            {saving ? strings.edit.saving : strings.edit.save}
          </button>
        </div>
      </div>

      {notice ? <Banner tone="success" title={notice} /> : null}
      {formError ? <Banner tone="danger" alert title={formError} /> : null}
      {conflict ? (
        <Banner tone="warning" icon="warning" alert title={strings.dietary.conflictTitle}>
          <button type="button" className="wk-link" onClick={onReload}>
            {strings.dietary.reload}
          </button>
        </Banner>
      ) : null}

      <section className="wx-panel">
        <AvatarPicker displayName={displayName} currentUrl={saved.avatarUrl} serverError={avatarError} onChange={setAvatar} />
        <TextField
          label={strings.edit.displayName}
          name="displayName"
          value={displayName}
          onChange={(event) => setDisplayName(event.target.value)}
          error={errors.displayName}
          helper={strings.edit.displayNameHelper}
          counter={`${codePointLength(stripped(displayName))}/${NAME_MAX}`}
        />
        <div className="wk-field">
          <label className="wk-label" htmlFor="profile-bio">
            {strings.edit.bio}
          </label>
          <textarea
            className="wk-textarea"
            id="profile-bio"
            value={bio}
            onChange={(event) => setBio(event.target.value)}
            aria-invalid={errors.bio ? true : undefined}
            aria-describedby="profile-bio-help"
          />
          <p className={errors.bio ? 'wk-error' : 'wk-helper'} id="profile-bio-help">
            <span>{errors.bio ?? strings.edit.bioHelper}</span>
            <span>
              {codePointLength(stripped(bio))}/{BIO_MAX}
            </span>
          </p>
        </div>
      </section>

      <Banner tone="info" icon="eye" title={strings.edit.publicNote}>
        {strings.edit.publicNoteBody}
      </Banner>
    </form>
  );
}

/** W-PF04 Hồ sơ công khai (US2-1, US2-2): name, picture and bio, saved together (FR-002 – FR-004). */
export function EditProfilePage() {
  const { state, reload } = useMyProfile();
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
      {state.status === 'ready' ? <EditProfileForm key={state.data.version} profile={state.data} onReload={reload} /> : null}
    </AccountLayout>
  );
}
