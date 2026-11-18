import { useMemo, useRef, useState, type DragEvent } from 'react';
import { validateFiles } from '../lib/validation';
import { strings } from '../strings.vi';
import { REVIEW_LIMITS, type Cooksnap } from '../types';
import { Icon } from './Icon';

interface CooksnapUploaderProps {
  /** Photos already saved on the review being edited and not removed yet. */
  existing: Cooksnap[];
  files: File[];
  onRemoveExisting: (id: number) => void;
  onFilesChange: (files: File[]) => void;
  /** Error from the server for `cooksnapFiles`. */
  serverError?: string;
  labelId: string;
}

function objectUrl(file: File): string {
  try {
    return URL.createObjectURL(file);
  } catch {
    return '';
  }
}

/** Preview of a picked file; the object URL is released once the image has loaded. */
function FilePreview({ file, alt }: { file: File; alt: string }) {
  const url = useMemo(() => objectUrl(file), [file]);
  return (
    <img
      src={url}
      alt={alt}
      onLoad={() => URL.revokeObjectURL?.(url)}
      style={{ width: '100%', height: '100%', objectFit: 'cover' }}
    />
  );
}

/**
 * Cooksnap picker of W-RV02 (T055, US2, FR-006/FR-007): up to 3 photos in all (kept + new), JPEG/PNG/WebP up to
 * 5 MB each, checked before upload; previews with a remove button; files can also be dropped on the add tile.
 */
export function CooksnapUploader({
  existing,
  files,
  onRemoveExisting,
  onFilesChange,
  serverError,
  labelId,
}: CooksnapUploaderProps) {
  const input = useRef<HTMLInputElement>(null);
  const [error, setError] = useState<string | null>(null);
  const total = existing.length + files.length;
  const full = total >= REVIEW_LIMITS.maxCooksnaps;

  const add = (picked: File[]) => {
    const { accepted, error: problem } = validateFiles(picked, REVIEW_LIMITS.maxCooksnaps - total);
    setError(problem);
    if (accepted.length > 0) onFilesChange([...files, ...accepted]);
  };

  const onDrop = (event: DragEvent<HTMLButtonElement>) => {
    event.preventDefault();
    if (!full) add(Array.from(event.dataTransfer.files));
  };

  const shown = error ?? serverError;

  return (
    <>
      <div className="wk-snaps" style={{ gridTemplateColumns: 'repeat(auto-fill, minmax(96px, 120px))' }}>
        {existing.map((snap, index) => {
          const n = index + 1;
          return (
            <div key={`saved-${snap.id}`} className="wk-photo" style={{ position: 'relative' }}>
              <img
                src={snap.thumbnailUrl}
                alt={strings.form.previewAlt(n)}
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
              />
              <RemoveButton label={strings.form.removePhoto(n)} onClick={() => onRemoveExisting(snap.id)} />
            </div>
          );
        })}
        {files.map((file, index) => {
          const n = existing.length + index + 1;
          return (
            <div key={`new-${index}-${file.name}`} className="wk-photo" style={{ position: 'relative' }}>
              <FilePreview file={file} alt={strings.form.previewAlt(n)} />
              <RemoveButton
                label={strings.form.removePhoto(n)}
                onClick={() => {
                  setError(null);
                  onFilesChange(files.filter((_, i) => i !== index));
                }}
              />
            </div>
          );
        })}
        <button
          type="button"
          className="wk-photo"
          aria-label={strings.form.addPhoto}
          aria-describedby={labelId}
          title={full ? strings.form.errors.tooManyFiles : undefined}
          disabled={full}
          onClick={() => input.current?.click()}
          onDragOver={(event) => event.preventDefault()}
          onDrop={onDrop}
          style={{
            aspectRatio: '1',
            borderRadius: 12,
            border: '1.5px dashed var(--color-border-strong)',
            font: 'inherit',
            cursor: full ? 'not-allowed' : 'pointer',
            opacity: full ? 0.5 : 1,
          }}
        >
          <Icon name="camera" />
        </button>
      </div>
      <input
        ref={input}
        type="file"
        accept={REVIEW_LIMITS.acceptedTypes.join(',')}
        multiple
        hidden
        tabIndex={-1}
        aria-hidden="true"
        onChange={(event) => {
          add(Array.from(event.target.files ?? []));
          event.target.value = '';
        }}
      />
      {shown && (
        <p className="wk-error">
          <Icon name="alert" />
          {shown}
        </p>
      )}
    </>
  );
}

function RemoveButton({ label, onClick }: { label: string; onClick: () => void }) {
  return (
    <button
      type="button"
      className="wk-icon-btn wk-icon-btn--overlay"
      aria-label={label}
      onClick={onClick}
      style={{ position: 'absolute', top: 2, right: 2, width: 44, height: 44 }}
    >
      <Icon name="close" />
    </button>
  );
}
