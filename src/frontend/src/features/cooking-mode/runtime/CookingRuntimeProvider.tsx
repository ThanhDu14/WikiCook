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
  const [widgetHidden, setWidgetHidden] = useState(false);
  const [backgroundSessions, setBackgroundSessions] = useState<CookingSession[]>([]);

  useEffect(() => {
    runtime.setCallbacks({
      onStorageQuotaExceeded: () => setHasStorageWarning(true),
      onTabConflict: () => setHasTabConflict(true),
      onClockJump: () => setHasClockJumpWarning(true),
    });

    const unsubscribe = runtime.subscribe((updatedSession) => {
      setSession(updatedSession);
      const owned = runtime.getSessions();
      setBackgroundSessions(owned.filter((item) => item.phase === 'BACKGROUND'));
      sharedAlarmCoordinator.evaluateAlarms(owned.flatMap((item) => item.timers));
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

  const handleReturnFromBackground = (recipeId: string) => {
    if (typeof window !== 'undefined') window.location.assign(`/cooking/${encodeURIComponent(recipeId)}`);
  };

  const backgroundRecipeIds = backgroundSessions.map((item) => item.recipeId).join(',');
  useEffect(() => {
    // A changed set of background sessions makes the dismissed widget visible again.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setWidgetHidden(false);
  }, [backgroundRecipeIds]);

  const handleDismissBackground = () => {
    setWidgetHidden(true);
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
      {!widgetHidden && backgroundSessions.some((item) => item.timers.length > 0) && (
        <BackgroundTimerWidget
          visible={true}
          timers={backgroundSessions.flatMap((item) => item.timers)}
          onReturnToCooking={() => handleReturnFromBackground(backgroundSessions[0].recipeId)}
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
