import React, { createContext, useContext, useEffect, useState } from 'react';
import type { CookingSession, RecipeSnapshot, StorageLoadResult } from '../domain/sessionModel';
import { CookingRuntimeManager, sharedCookingRuntime } from './cookingRuntime';
import { sharedAlarmCoordinator } from './alarmCoordinator';
import { BackgroundTimerWidget } from '../components/BackgroundTimerWidget';

export interface CookingRuntimeContextValue {
  session: CookingSession | null;
  runtime: CookingRuntimeManager;
  startSession: (snapshot: RecipeSnapshot) => CookingSession;
  resumeSession: (recipeId: string) => void;
  checkResumable: (recipeId: string) => StorageLoadResult;
  exitSession: (keepTimers: boolean) => void;
  completeCooking: (onCompleted?: (recipeId: string) => void) => boolean;
  hasTabConflict: boolean;
  hasStorageWarning: boolean;
  hasClockJumpWarning: boolean;
  clearWarnings: () => void;
}

const CookingRuntimeContext = createContext<CookingRuntimeContextValue | null>(null);

export interface CookingRuntimeProviderProps {
  children: React.ReactNode;
  customRuntime?: CookingRuntimeManager;
}

export function CookingRuntimeProvider({
  children,
  customRuntime,
}: CookingRuntimeProviderProps): React.ReactElement {
  const runtime = customRuntime || sharedCookingRuntime;
  const [session, setSession] = useState<CookingSession | null>(runtime.getActiveSession());
  const [hasTabConflict, setHasTabConflict] = useState(false);
  const [hasStorageWarning, setHasStorageWarning] = useState(false);
  const [hasClockJumpWarning, setHasClockJumpWarning] = useState(false);

  useEffect(() => {
    runtime.setCallbacks({
      onStorageQuotaExceeded: () => setHasStorageWarning(true),
      onTabConflict: () => setHasTabConflict(true),
      onClockJump: () => setHasClockJumpWarning(true),
    });

    const unsubscribe = runtime.subscribe((updatedSession) => {
      setSession(updatedSession);
      if (updatedSession?.timers) {
        sharedAlarmCoordinator.evaluateAlarms(updatedSession.timers);
      } else {
        sharedAlarmCoordinator.cleanup();
      }
    });

    const handleBeforeUnload = () => {
      const active = runtime.getActiveSession();
      if (active) {
        runtime.saveSession(active);
      }
    };

    if (typeof window !== 'undefined') {
      window.addEventListener('beforeunload', handleBeforeUnload);
    }

    return () => {
      unsubscribe();
      sharedAlarmCoordinator.cleanup();
      if (typeof window !== 'undefined') {
        window.removeEventListener('beforeunload', handleBeforeUnload);
      }
    };
  }, [runtime]);

  const handleReturnFromBackground = () => {
    if (session) {
      const check = runtime.checkResumableSession(session.recipeId);
      if (check.kind === 'RESUMABLE') {
        runtime.resumeSession(check.value);
      }
      if (typeof window !== 'undefined' && !window.location.pathname.startsWith('/cooking/')) {
        window.location.assign(`/cooking/${session.recipeId}`);
      }
    }
  };

  const handleDismissBackground = () => {
    runtime.exitSession(false);
    sharedAlarmCoordinator.cleanup();
  };

  const value: CookingRuntimeContextValue = {
    session,
    runtime,
    startSession: (snapshot) => runtime.startSession(snapshot),
    resumeSession: (recipeId) => {
      const check = runtime.checkResumableSession(recipeId);
      if (check.kind === 'RESUMABLE') {
        runtime.resumeSession(check.value);
      }
    },
    checkResumable: (recipeId) => runtime.checkResumableSession(recipeId),
    exitSession: (keepTimers) => runtime.exitSession(keepTimers),
    completeCooking: (onCompleted) => runtime.completeSession(onCompleted),
    hasTabConflict,
    hasStorageWarning,
    hasClockJumpWarning,
    clearWarnings: () => {
      setHasTabConflict(false);
      setHasStorageWarning(false);
      setHasClockJumpWarning(false);
    },
  };

  return (
    <CookingRuntimeContext.Provider value={value}>
      {children}
      {session?.phase === 'BACKGROUND' && session.timers.length > 0 && (
        <BackgroundTimerWidget
          visible={true}
          timers={session.timers}
          onReturnToCooking={handleReturnFromBackground}
          onDismiss={handleDismissBackground}
        />
      )}
    </CookingRuntimeContext.Provider>
  );
}

export function useCookingRuntime(): CookingRuntimeContextValue {
  const ctx = useContext(CookingRuntimeContext);
  if (!ctx) {
    throw new Error('useCookingRuntime must be used within a CookingRuntimeProvider');
  }
  return ctx;
}
