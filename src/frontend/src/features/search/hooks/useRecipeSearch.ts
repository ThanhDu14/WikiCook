import { useCallback, useEffect, useRef, useState } from 'react';
import { searchApi } from '../api/searchApi';
import { toSearchString } from '../lib/searchState';
import type { SearchResponse, SearchState } from '../types';

export interface UseRecipeSearchResult {
  data: SearchResponse | null;
  loading: boolean;
  error: Error | null;
  retry: () => void;
}

/**
 * Hook to manage recipe search lifecycle:
 * - Cancels/aborts older out-of-order requests (FR-026).
 * - Keeps previous data while reloading for a new state.
 * - Provides retry() function to recover from errors.
 */
export function useRecipeSearch(state: SearchState): UseRecipeSearchResult {
  const [data, setData] = useState<SearchResponse | null>(null);
  const [loadedKey, setLoadedKey] = useState<string | null>(null);
  const [error, setError] = useState<Error | null>(null);
  const [retryCount, setRetryCount] = useState<number>(0);

  const requestIdRef = useRef<number>(0);
  const abortControllerRef = useRef<AbortController | null>(null);

  const queryString = toSearchString(state);
  const currentKey = `${queryString}#${retryCount}`;
  const loading = loadedKey !== currentKey;

  const retry = useCallback(() => {
    setRetryCount((c) => c + 1);
  }, []);

  useEffect(() => {
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }
    const controller = new AbortController();
    abortControllerRef.current = controller;

    const currentRequestId = ++requestIdRef.current;

    searchApi
      .searchRecipes(queryString)
      .then((res) => {
        if (controller.signal.aborted || currentRequestId !== requestIdRef.current) {
          return;
        }
        setData(res);
        setError(null);
        setLoadedKey(currentKey);
      })
      .catch((err: unknown) => {
        if (controller.signal.aborted || currentRequestId !== requestIdRef.current) {
          return;
        }
        setError(err instanceof Error ? err : new Error(String(err)));
        setLoadedKey(currentKey);
      });

    return () => {
      controller.abort();
    };
  }, [queryString, retryCount, currentKey]);

  return {
    data,
    loading,
    error,
    retry,
  };
}
