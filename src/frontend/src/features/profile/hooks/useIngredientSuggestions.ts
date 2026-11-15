import { useEffect, useState } from 'react';
import { suggestIngredients } from '../api/dietaryApi';
import type { NamedItem } from '../types';

const DEBOUNCE_MS = 150;
const MIN_LENGTH = 2;

export interface SuggestionsState {
  items: NamedItem[];
  loading: boolean;
  failed: boolean;
}

interface Answer {
  query: string;
  items: NamedItem[];
  failed: boolean;
}

/**
 * Suggestions for the ingredient typed so far (US1-3): waits 150 ms after the last key, asks nothing for fewer than
 * two characters, and cancels the request of an older query when a newer one starts, so a late answer never replaces a
 * newer one (NFR-001). An answer counts only while it is for the text now in the box.
 */
export function useIngredientSuggestions(query: string): SuggestionsState {
  const [answer, setAnswer] = useState<Answer | null>(null);
  const text = query.trim();
  const active = [...text].length >= MIN_LENGTH;

  useEffect(() => {
    if (!active) return;
    const controller = new AbortController();
    const timer = setTimeout(() => {
      suggestIngredients(text, controller.signal)
        .then((items) => setAnswer({ query: text, items, failed: false }))
        .catch((error: unknown) => {
          if (controller.signal.aborted || (error instanceof DOMException && error.name === 'AbortError')) return;
          setAnswer({ query: text, items: [], failed: true });
        });
    }, DEBOUNCE_MS);
    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [text, active]);

  const current = active && answer?.query === text ? answer : null;
  return { items: current?.items ?? [], loading: active && current === null, failed: current?.failed ?? false };
}
