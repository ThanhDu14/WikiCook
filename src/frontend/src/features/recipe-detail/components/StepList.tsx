import { useState } from 'react';
import { timerLabel } from '../lib/format';
import type { RecipeStep } from '../types';

function StepImage({ url, alt }: { url: string; alt: string }) {
  const [failed, setFailed] = useState(false);
  if (failed) return null;
  return <img src={url} alt={alt} loading="lazy" onError={() => setFailed(true)} />;
}

/** "Các bước" of W-NU01: number, title, text, timer chip, and the step's photos. All text is rendered as text. */
export function StepList({ steps }: { steps: RecipeStep[] }) {
  return (
    <ol className="wx-steps">
      {steps.map((step) => (
        <li key={step.position} className="wx-step">
          <span className="wx-step__num" aria-hidden="true">
            {step.position}
          </span>
          <div className="wx-step__body">
            {step.title && <h3 className="wk-h3 wx-step__title">{step.title}</h3>}
            {step.content && <p className="wx-step__text">{step.content}</p>}
            {step.timerSeconds != null && step.timerSeconds > 0 && (
              <span className="wk-chip wx-step__timer">{`⏱ ${timerLabel(step.timerSeconds)}`}</span>
            )}
            {step.imageUrls.length > 0 && (
              <div className="wx-step__photos">
                {step.imageUrls.map((url, index) => (
                  <StepImage key={url} url={url} alt={`Bước ${step.position} – ảnh ${index + 1}`} />
                ))}
              </div>
            )}
          </div>
        </li>
      ))}
    </ol>
  );
}
