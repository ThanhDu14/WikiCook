import { useState, type FormEvent } from 'react';
import { ApiError } from '@/shared/api/client';
import { Banner } from '@/shared/ui/Banner';
import { UnsavedChangesDialog } from '@/features/auth/components/UnsavedChangesDialog';
import { useUnsavedChangesBlocker } from '@/features/auth/hooks/useUnsavedChangesBlocker';
import { fieldMessages, problemCode, retryMinutes } from '@/features/auth/lib/problem';
import { ackDietaryNotices, getDietaryProfile, saveDietaryProfile } from '../api/dietaryApi';
import { AccountLayout } from '../components/AccountLayout';
import { AllergenPicker } from '../components/AllergenPicker';
import { CatalogueNotices } from '../components/CatalogueNotices';
import { CuisinePicker } from '../components/CuisinePicker';
import { DietaryEmptyState } from '../components/DietaryEmptyState';
import { DietTypeSelect } from '../components/DietTypeSelect';
import { ExcludedIngredientPicker } from '../components/ExcludedIngredientPicker';
import { NumberField } from '../components/NumberField';
import { ProfileSkeleton } from '../components/ProfileSkeleton';
import { useDietaryProfile, type DietaryData } from '../hooks/useDietaryProfile';
import { parseWholeNumber } from '../lib/wholeNumber';
import { strings } from '../strings.vi';
import {
  COOK_TIME_RANGE,
  HOUSEHOLD_SIZE_RANGE,
  type DietaryNotice,
  type DietaryProfile,
  type DietType,
  type NamedItem,
} from '../types';

interface Draft {
  allergenIds: Set<number>;
  excluded: NamedItem[];
  dietType: DietType;
  cuisineIds: Set<number>;
  household: string;
  cookTime: string;
}

const draftOf = (profile: DietaryProfile): Draft => ({
  allergenIds: new Set(profile.allergens.map((item) => item.id)),
  excluded: profile.excludedIngredients,
  dietType: profile.dietType,
  cuisineIds: new Set(profile.preferredCuisines.map((item) => item.id)),
  household: profile.householdSize == null ? '' : String(profile.householdSize),
  cookTime: profile.maxCookTimeMinutes == null ? '' : String(profile.maxCookTimeMinutes),
});

/** Same content gives the same key, whatever order it was picked in: "unsaved changes" means a different key. */
const keyOf = (draft: Draft) =>
  JSON.stringify([
    [...draft.allergenIds].sort((a, b) => a - b),
    draft.excluded.map((item) => item.id).sort((a, b) => a - b),
    draft.dietType,
    [...draft.cuisineIds].sort((a, b) => a - b),
    draft.household.trim(),
    draft.cookTime.trim(),
  ]);

const toggled = (set: Set<number>, id: number) => {
  const next = new Set(set);
  if (!next.delete(id)) next.add(id);
  return next;
};

function DietaryForm({ data, onReload }: { data: DietaryData; onReload: () => void }) {
  const { options } = data;
  const [saved, setSaved] = useState(data.profile);
  const [draft, setDraft] = useState(() => draftOf(data.profile));
  const [started, setStarted] = useState(!data.profile.empty);
  const [notices, setNotices] = useState<DietaryNotice[]>(data.profile.notices);
  const [errors, setErrors] = useState<{ household?: string; cookTime?: string }>({});
  const [message, setMessage] = useState<{ kind: 'ok' | 'error'; text: string } | null>(null);
  const [conflict, setConflict] = useState(false);
  const [saving, setSaving] = useState(false);

  const savedKey = keyOf(draftOf(saved));
  const dirty = keyOf(draft) !== savedKey;
  const blocker = useUnsavedChangesBlocker(dirty && !saving);

  async function save(version: number) {
    const household = parseWholeNumber(draft.household, HOUSEHOLD_SIZE_RANGE.min, HOUSEHOLD_SIZE_RANGE.max);
    const cookTime = parseWholeNumber(draft.cookTime, COOK_TIME_RANGE.min, COOK_TIME_RANGE.max);
    const found = {
      household: household.valid ? undefined : strings.dietary.householdError,
      cookTime: cookTime.valid ? undefined : strings.dietary.maxCookTimeError,
    };
    setErrors(found);
    setMessage(null);
    setConflict(false);
    if (found.household || found.cookTime) return;

    setSaving(true);
    try {
      const next = await saveDietaryProfile({
        allergenIds: [...draft.allergenIds],
        excludedIngredientIds: draft.excluded.map((item) => item.id),
        dietType: draft.dietType,
        preferredCuisineIds: [...draft.cuisineIds],
        householdSize: household.value,
        maxCookTimeMinutes: cookTime.value,
        version,
      });
      setSaved(next);
      setDraft(draftOf(next));
      setNotices(next.notices);
      setMessage({ kind: 'ok', text: strings.dietary.saved });
    } catch (error) {
      const code = problemCode(error);
      if (code === 'PROFILE_CHANGED') {
        setConflict(true);
      } else if (code === 'VALIDATION_ERROR') {
        const fields = fieldMessages(error);
        setErrors({
          household: fields.householdSize ? strings.dietary.householdError : undefined,
          cookTime: fields.maxCookTimeMinutes ? strings.dietary.maxCookTimeError : undefined,
        });
        if (!fields.householdSize && !fields.maxCookTimeMinutes) setMessage({ kind: 'error', text: strings.errors.generic });
      } else if (code === 'PROFILE_RATE_LIMITED') {
        setMessage({ kind: 'error', text: strings.dietary.rateLimited(retryMinutes(error)) });
      } else if (code === 'PROFILE_UNAVAILABLE') {
        setMessage({ kind: 'error', text: strings.dietary.unavailable });
      } else if (error instanceof ApiError) {
        setMessage({ kind: 'error', text: strings.errors.generic });
      } else {
        // the typed values stay in the form, so "Lưu hồ sơ" again is the retry
        setMessage({ kind: 'error', text: strings.errors.network });
      }
    } finally {
      setSaving(false);
    }
  }

  async function overwrite() {
    try {
      const latest = await getDietaryProfile();
      await save(latest.version);
    } catch {
      setMessage({ kind: 'error', text: strings.errors.network });
    }
  }

  function submit(event: FormEvent) {
    event.preventDefault();
    if (!saving) void save(saved.version);
  }

  async function closeNotice(notice: DietaryNotice) {
    setNotices((current) => current.filter((item) => item.id !== notice.id));
    try {
      await ackDietaryNotices([notice.id]);
    } catch {
      // it will be shown again on the next visit, which is the safe side
    }
  }

  return (
    <form onSubmit={submit} noValidate className="sx-stack" style={{ gap: 24 }}>
      <UnsavedChangesDialog blocker={blocker} />
      <div className="wx-head">
        <div>
          <h1>{strings.dietary.title}</h1>
        </div>
        {started ? (
          <div className="sx-row" style={{ flexWrap: 'wrap' }}>
            <button type="button" className="wk-btn wk-btn--secondary" disabled={!dirty || saving} onClick={() => setDraft(draftOf(saved))}>
              {strings.dietary.cancel}
            </button>
            <button
              type="submit"
              className={`wk-btn wk-btn--primary${saving ? ' is-loading' : ''}`}
              disabled={saving}
              aria-busy={saving || undefined}
            >
              {saving ? strings.dietary.saving : strings.dietary.save}
            </button>
          </div>
        ) : null}
      </div>

      <CatalogueNotices notices={notices} onClose={closeNotice} />

      {message ? (
        <Banner tone={message.kind === 'ok' ? 'success' : 'danger'} alert={message.kind === 'error'} title={message.text}>
          {message.kind === 'error' ? (
            <button type="submit" className="wk-link" disabled={saving}>
              {strings.retry}
            </button>
          ) : null}
        </Banner>
      ) : null}
      {conflict ? (
        <Banner tone="warning" icon="warning" alert title={strings.dietary.conflictTitle}>
          {strings.dietary.conflictBody}{' '}
          <button type="button" className="wk-link" onClick={onReload}>
            {strings.dietary.reload}
          </button>{' '}
          <button type="button" className="wk-link" onClick={overwrite} disabled={saving}>
            {strings.dietary.overwrite}
          </button>
        </Banner>
      ) : null}

      {!started ? (
        <DietaryEmptyState onStart={() => setStarted(true)} />
      ) : (
        <>
          <section className="wx-panel">
            <div>
              <p className="wk-eyebrow">{strings.dietary.mandatory}</p>
              <p className="wk-small wk-muted">{strings.dietary.mandatoryHint}</p>
            </div>
            <AllergenPicker
              options={options.allergens}
              selected={draft.allergenIds}
              onToggle={(id) => setDraft((current) => ({ ...current, allergenIds: toggled(current.allergenIds, id) }))}
            />
            <ExcludedIngredientPicker
              selected={draft.excluded}
              onAdd={(item) => setDraft((current) => (current.excluded.some((x) => x.id === item.id) ? current : { ...current, excluded: [...current.excluded, item] }))}
              onRemove={(id) => setDraft((current) => ({ ...current, excluded: current.excluded.filter((item) => item.id !== id) }))}
            />
          </section>

          <section className="wx-panel">
            <p className="wk-eyebrow">{strings.dietary.preferences}</p>
            <div className="wx-form" style={{ gridTemplateColumns: 'repeat(3, minmax(0, 1fr))' }}>
              <DietTypeSelect options={options.dietTypes} value={draft.dietType} onChange={(dietType) => setDraft((current) => ({ ...current, dietType }))} />
              <NumberField
                label={strings.dietary.householdSize}
                value={draft.household}
                onChange={(event) => {
                  setDraft((current) => ({ ...current, household: event.target.value }));
                  if (errors.household) setErrors((prev) => ({ ...prev, household: undefined }));
                }}
                error={errors.household}
                helper={strings.dietary.householdHelper}
              />
              <NumberField
                label={strings.dietary.maxCookTime}
                value={draft.cookTime}
                onChange={(event) => {
                  setDraft((current) => ({ ...current, cookTime: event.target.value }));
                  if (errors.cookTime) setErrors((prev) => ({ ...prev, cookTime: undefined }));
                }}
                error={errors.cookTime}
                helper={strings.dietary.maxCookTimeHelper}
              />
              <CuisinePicker
                options={options.cuisines}
                selected={draft.cuisineIds}
                onToggle={(id) => setDraft((current) => ({ ...current, cuisineIds: toggled(current.cuisineIds, id) }))}
              />
            </div>
          </section>
        </>
      )}
    </form>
  );
}

/**
 * W-PF02 / W-PF03 Hồ sơ ăn uống (US1): allergies and ingredients to avoid (hard rules), diet type, household size,
 * cooking time and cuisines (preferences). Loading, empty, error and data states are all shown (FR-039); the typed
 * values survive a network failure and a refused save; a save made elsewhere meanwhile is a conflict you resolve.
 */
export function DietaryProfilePage() {
  const { state, reload } = useDietaryProfile();
  const unavailable = state.status === 'error' && state.error instanceof ApiError && state.error.problem.code === 'PROFILE_UNAVAILABLE';

  return (
    <AccountLayout>
      {state.status === 'loading' ? <ProfileSkeleton /> : null}
      {state.status === 'error' ? (
        <div className="wk-state wk-state--error" role="alert">
          <p>{unavailable ? strings.dietary.unavailable : strings.dietary.loadError}</p>
          <button type="button" className="wk-btn wk-btn--primary" onClick={reload}>
            {strings.retry}
          </button>
        </div>
      ) : null}
      {state.status === 'ready' ? <DietaryForm key={state.data.profile.version} data={state.data} onReload={reload} /> : null}
    </AccountLayout>
  );
}
