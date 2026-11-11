import { describe, it, expect, beforeEach } from 'vitest';
import {
  saveSessionToStorage,
  loadSessionFromStorage,
  clearSessionFromStorage,
  hasResumableSession,
} from '../adapters/sessionPersistence';
import { createInitialSession } from '../domain/sessionReducer';
import { mockPhoBoSnapshot } from './fixtures/recipeFixtures';
import type { CookingSession } from '../domain/sessionModel';

describe('sessionPersistence (T041 - US5)', () => {
  const BASE_NOW = 1731300000000;
  let testSession: CookingSession;

  beforeEach(() => {
    localStorage.clear();
    testSession = createInitialSession(mockPhoBoSnapshot, 'sess-persist-1', BASE_NOW);
  });

  it('saves session wrapped in StorageEnvelopeV1 and loads it back accurately', () => {
    saveSessionToStorage(testSession, 'tab-writer-1', 1, BASE_NOW);

    const loaded = loadSessionFromStorage(mockPhoBoSnapshot.recipeId, BASE_NOW);
    expect(loaded.kind).toBe('RESUMABLE');
    if (loaded.kind === 'RESUMABLE') {
      expect(loaded.value.schemaVersion).toBe(1);
      expect(loaded.value.writerId).toBe('tab-writer-1');
      expect(loaded.value.revision).toBe(1);
      expect(loaded.value.session.sessionId).toBe('sess-persist-1');
      expect(loaded.value.session.recipeId).toBe(mockPhoBoSnapshot.recipeId);
      expect(loaded.value.session.expiresAt).toBe(BASE_NOW + 4 * 3600 * 1000);
    }
  });

  it('returns NONE when no session is stored for given recipeId', () => {
    const loaded = loadSessionFromStorage('recipe-non-existent', BASE_NOW);
    expect(loaded.kind).toBe('NONE');
    expect(hasResumableSession('recipe-non-existent', BASE_NOW)).toBe(false);
  });

  it('purges session and returns EXPIRED when session exceeds 4-hour TTL (A08)', () => {
    saveSessionToStorage(testSession, 'tab-writer-1', 1, BASE_NOW);

    // 4 hours + 1 ms later
    const expiredNow = BASE_NOW + 4 * 3600 * 1000 + 1;
    const loaded = loadSessionFromStorage(mockPhoBoSnapshot.recipeId, expiredNow);

    expect(loaded.kind).toBe('EXPIRED');
    expect(localStorage.getItem(`cooking_session_${mockPhoBoSnapshot.recipeId}`)).toBeNull();
    expect(hasResumableSession(mockPhoBoSnapshot.recipeId, expiredNow)).toBe(false);
  });

  it('handles corrupted JSON gracefully and returns INVALID', () => {
    localStorage.setItem(`cooking_session_${mockPhoBoSnapshot.recipeId}`, '{ corrupted json ...');

    const loaded = loadSessionFromStorage(mockPhoBoSnapshot.recipeId, BASE_NOW);
    expect(loaded.kind).toBe('INVALID');
    expect(hasResumableSession(mockPhoBoSnapshot.recipeId, BASE_NOW)).toBe(false);
  });

  it('returns UNSUPPORTED_VERSION if schemaVersion is not 1', () => {
    const v2Envelope = {
      schemaVersion: 2,
      writerId: 'tab-future',
      revision: 1,
      savedAt: BASE_NOW,
      session: testSession,
    };
    localStorage.setItem(
      `cooking_session_${mockPhoBoSnapshot.recipeId}`,
      JSON.stringify(v2Envelope),
    );

    const loaded = loadSessionFromStorage(mockPhoBoSnapshot.recipeId, BASE_NOW);
    expect(loaded.kind).toBe('UNSUPPORTED_VERSION');
    expect(hasResumableSession(mockPhoBoSnapshot.recipeId, BASE_NOW)).toBe(false);
  });

  it('isolates sessions between different recipes without cross-over', () => {
    const recipeA = 'recipe-pho-bo';
    const recipeB = 'recipe-bun-cha';

    const sessionA: CookingSession = {
      ...testSession,
      recipeId: recipeA,
      sessionId: 'sess-A',
    };
    const sessionB: CookingSession = {
      ...testSession,
      recipeId: recipeB,
      sessionId: 'sess-B',
    };

    saveSessionToStorage(sessionA, 'tab-1', 1, BASE_NOW);
    saveSessionToStorage(sessionB, 'tab-1', 1, BASE_NOW);

    const loadedA = loadSessionFromStorage(recipeA, BASE_NOW);
    const loadedB = loadSessionFromStorage(recipeB, BASE_NOW);

    expect(loadedA.kind).toBe('RESUMABLE');
    expect(loadedB.kind).toBe('RESUMABLE');
    if (loadedA.kind === 'RESUMABLE' && loadedB.kind === 'RESUMABLE') {
      expect(loadedA.value.session.sessionId).toBe('sess-A');
      expect(loadedB.value.session.sessionId).toBe('sess-B');
    }

    clearSessionFromStorage(recipeA);
    expect(loadSessionFromStorage(recipeA, BASE_NOW).kind).toBe('NONE');
    expect(loadSessionFromStorage(recipeB, BASE_NOW).kind).toBe('RESUMABLE');
  });
});
