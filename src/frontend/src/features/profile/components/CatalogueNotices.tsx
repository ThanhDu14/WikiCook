import { Banner } from '@/shared/ui/Banner';
import { strings } from '../strings.vi';
import type { DietaryNotice } from '../types';

function textOf(notice: DietaryNotice) {
  const t = strings.dietary.notice;
  switch (notice.kind) {
    case 'INGREDIENT_MERGED':
      return { title: t.mergedTitle, body: t.merged(notice) };
    case 'INGREDIENT_REMOVED':
      return { title: t.removedTitle, body: t.ingredientRemoved(notice) };
    case 'ALLERGEN_REMOVED':
      return { title: t.removedTitle, body: t.allergenRemoved(notice) };
  }
}

/** Changes of the catalogue that touched this profile (merged or removed items), shown until closed (FR-019). */
export function CatalogueNotices({ notices, onClose }: { notices: DietaryNotice[]; onClose: (notice: DietaryNotice) => void }) {
  if (notices.length === 0) return null;
  return (
    <div className="sx-stack" style={{ gap: 8 }}>
      {notices.map((notice) => {
        const { title, body } = textOf(notice);
        return (
          <Banner key={notice.id} tone="info" title={title}>
            {body}{' '}
            <button type="button" className="wk-link" onClick={() => onClose(notice)}>
              {strings.dietary.notice.close}
            </button>
          </Banner>
        );
      })}
    </div>
  );
}
