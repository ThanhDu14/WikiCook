import React, { useEffect, useState, useRef } from 'react';
import { useCookingRuntime } from '../runtime/CookingRuntimeProvider';
import { COOKING_VI } from '../i18n/vi';
import { wakeLockAdapter, fullscreenAdapter, getAudioSynth } from '../adapters/browserCapabilities';
import { sharedAlarmCoordinator } from '../runtime/alarmCoordinator';
import { sharedSpeechAdapter, type VoiceCommand } from '../adapters/speechAdapter';
import { ExitSessionDialog } from './ExitSessionDialog';
import { StepNavigation } from './StepNavigation';
import { IngredientDrawer } from './IngredientDrawer';
import { StepQuickTimers } from './StepQuickTimers';
import { TimerDrawer } from './TimerDrawer';
import { CustomTimerDialog } from './CustomTimerDialog';
import { ResumeSessionDialog } from './ResumeSessionDialog';
import type { RecipeSnapshot, StorageEnvelopeV1 } from '../domain/sessionModel';

export interface CookingViewProps {
  initialSnapshot?: RecipeSnapshot;
  onClose?: () => void;
  onFinish?: (recipeId: string) => void;
  onHandoffToReview?: (recipeId: string) => void;
  children?: React.ReactNode;
}

export function CookingView({
  initialSnapshot,
  onClose,
  onFinish,
  onHandoffToReview,
  children,
}: CookingViewProps): React.ReactElement {
  const {
    session,
    runtime,
    hasTabConflict,
    hasStorageWarning,
    hasClockJumpWarning,
    clearWarnings,
  } = useCookingRuntime();

  const [wakeLockActive, setWakeLockActive] = useState<boolean>(false);
  const [wakeLockSupported, setWakeLockSupported] = useState<boolean>(true);
  const [isExitDialogOpen, setIsExitDialogOpen] = useState<boolean>(false);
  const [isIngredientDrawerOpen, setIsIngredientDrawerOpen] = useState<boolean>(false);
  const [isCustomTimerDialogOpen, setIsCustomTimerDialogOpen] = useState<boolean>(false);
  const [showCompletionBanner, setShowCompletionBanner] = useState<boolean>(false);
  const [isListening, setIsListening] = useState<boolean>(false);
  const [voiceNotice, setVoiceNotice] = useState<string | null>(null);
  const [resumableEnvelope, setResumableEnvelope] = useState<StorageEnvelopeV1 | null>(null);
  const [isResumeDialogOpen, setIsResumeDialogOpen] = useState<boolean>(false);
  const [failedImages, setFailedImages] = useState<Record<string, boolean>>({});
  const containerRef = useRef<HTMLDivElement>(null);
  const headingRef = useRef<HTMLHeadingElement>(null);

  const hydratedRecipeRef = useRef<string | null>(null);
  // Hydration is scoped to each route entry, not the previous active recipe.
  useEffect(() => {
    if (initialSnapshot && hydratedRecipeRef.current !== initialSnapshot.recipeId) {
      hydratedRecipeRef.current = initialSnapshot.recipeId;
      setShowCompletionBanner(false);
      const existing = runtime.getSessions().find((item) => item.recipeId === initialSnapshot.recipeId && !item.isCompleted);
      if (existing) {
        runtime.selectSession(initialSnapshot.recipeId);
        return;
      }
      const check = runtime.checkResumableSession(initialSnapshot.recipeId);
      if (check.kind === 'RESUMABLE') {
        setResumableEnvelope(check.value);
        setIsResumeDialogOpen(true);
      } else {
        runtime.startSession(initialSnapshot);
      }
    }
  }, [initialSnapshot, runtime, session]);

  const primeAudio = () => {
    void getAudioSynth().unlock().then((ok) => {
      if (!ok) setVoiceNotice('Âm thanh chưa được bật. Chạm nút hẹn giờ để thử lại; cảnh báo trực quan vẫn hoạt động.');
    });
  };

  const handleResumeConfirmed = () => {
    primeAudio();
    if (resumableEnvelope) {
      runtime.resumeSession(resumableEnvelope);
      setIsResumeDialogOpen(false);
      setResumableEnvelope(null);
    }
  };

  const handleStartOverConfirmed = () => {
    if (initialSnapshot) {
      runtime.startSession(initialSnapshot);
      setIsResumeDialogOpen(false);
      setResumableEnvelope(null);
    }
  };

  const sessionRef = useRef(session);
  sessionRef.current = session;
  const theme = session?.preferences.theme ?? 'LIGHT';
  const isHighContrast = theme === 'HIGH_CONTRAST_DARK';

  const handleVoiceCommand = (cmd: VoiceCommand) => {
    switch (cmd) {
      case 'NEXT_STEP':
        runtime.nextStep();
        break;
      case 'PREV_STEP':
        runtime.prevStep();
        break;
      case 'READ_STEP': {
        const active = sessionRef.current;
        const step = active?.snapshot.steps[active.currentStepIndex];
        if (step) {
          sharedSpeechAdapter.speakStep(step.instructionText);
        }
        break;
      }
      case 'OPEN_TIMER':
        setIsCustomTimerDialogOpen(true);
        break;
      case 'SILENCE_ALARM': {
        sessionRef.current?.timers.forEach((t) => {
          if (t.status === 'EXPIRED' && t.acknowledgedAt === null) {
            runtime.acknowledgeTimer(t.timerId);
          }
        });
        sharedAlarmCoordinator.stopAlarmLoop();
        break;
      }
      case 'UNKNOWN':
        setVoiceNotice(COOKING_VI.unrecognizedCommand);
        setTimeout(() => setVoiceNotice(null), 3000);
        break;
    }
  };

  const toggleVoiceListening = () => {
    if (isListening) {
      sharedSpeechAdapter.stopListening();
      setIsListening(false);
    } else {
      const ok = sharedSpeechAdapter.startListening({
        onCommand: (cmd) => handleVoiceCommand(cmd),
        onOffline: (msg) => {
          setVoiceNotice(msg);
          setTimeout(() => setVoiceNotice(null), 4000);
        },
        onIdleTimeout: () => setIsListening(false),
        onUnsupportedTts: (msg) => {
          setVoiceNotice(msg);
          setTimeout(() => setVoiceNotice(null), 4000);
        },
      });
      setIsListening(ok);
    }
  };

  // Request Wake Lock on mount and re-acquire when tab regains visibility (FR-003, FR-004)
  useEffect(() => {
    let isMounted = true;
    const supported = wakeLockAdapter.isSupported();
    setWakeLockSupported(supported);
    const unsubscribeWakeLock = wakeLockAdapter.subscribe((active) => {
      if (isMounted) {
        setWakeLockActive(active);
        if (!active) setWakeLockSupported(false);
      }
    });

    const acquireWakeLock = () => {
      if (!wakeLockAdapter.isSupported()) return;
      wakeLockAdapter.request().then((lock) => {
        if (isMounted) {
          setWakeLockActive(!!lock);
          setWakeLockSupported(!!lock);
        }
      });
    };

    if (supported) {
      acquireWakeLock();
    }

    const handleVisibilityChange = () => {
      if (typeof document !== 'undefined' && document.visibilityState === 'visible') {
        acquireWakeLock();
      }
    };

    if (typeof document !== 'undefined') {
      document.addEventListener('visibilitychange', handleVisibilityChange);
    }

    return () => {
      isMounted = false;
      unsubscribeWakeLock();
      if (typeof document !== 'undefined') {
        document.removeEventListener('visibilitychange', handleVisibilityChange);
      }
      wakeLockAdapter.release();
    };
  }, []);

  // Recognition must not retain microphone ownership after leaving the cooking view.
  useEffect(() => () => { sharedSpeechAdapter.stopListening(); }, []);

  // Focus management on step change
  useEffect(() => {
    headingRef.current?.focus();
  }, [session?.currentStepIndex]);

  if (!session || (initialSnapshot && session.recipeId !== initialSnapshot.recipeId) || isResumeDialogOpen) {
    return (
      <div className="flex min-h-screen items-center justify-center p-6 text-center">
        <p className="text-lg text-stone-500">Chưa có phiên nấu ăn nào được khởi tạo.</p>
        <ResumeSessionDialog
          isOpen={isResumeDialogOpen}
          stepNumber={(resumableEnvelope?.session.currentStepIndex ?? 0) + 1}
          onResume={handleResumeConfirmed}
          onStartOver={handleStartOverConfirmed}
        />
      </div>
    );
  }

  const { snapshot, currentStepIndex, timers } = session;
  const currentStep = snapshot.steps[currentStepIndex] || snapshot.steps[0];
  const totalSteps = snapshot.steps.length;

  const runningCount = timers.filter((t) => t.status === 'RUNNING').length;
  const pausedCount = timers.filter((t) => t.status === 'PAUSED').length;
  const expiredCount = timers.filter((t) => t.status === 'EXPIRED' && t.acknowledgedAt === null).length;
  const hasUnresolvedTimers = runningCount > 0 || pausedCount > 0 || expiredCount > 0;

  const hasStepMapping =
    Array.isArray(currentStep.ingredientRowIds) && currentStep.ingredientRowIds.length > 0;
  const stepMappedIngredients = hasStepMapping
    ? snapshot.ingredients.filter((ing) => currentStep.ingredientRowIds!.includes(ing.rowId))
    : [];

  const handleSilenceAllAlarms = () => {
    timers.forEach((t) => {
      if (t.status === 'EXPIRED' && t.acknowledgedAt === null) {
        runtime.acknowledgeTimer(t.timerId);
      }
    });
    sharedAlarmCoordinator.stopAlarmLoop();
  };

  const handleExitClick = () => {
    if (hasUnresolvedTimers) {
      setIsExitDialogOpen(true);
    } else {
      runtime.exitSession(false);
      onClose?.();
    }
  };

  const handleFinishClick = () => {
    if (hasUnresolvedTimers) {
      setIsExitDialogOpen(true);
    } else {
      runtime.completeSession((recipeId) => {
        setShowCompletionBanner(true);
        wakeLockAdapter.release();
        if (fullscreenAdapter.isFullscreen()) {
          fullscreenAdapter.exit();
        }
        onFinish?.(recipeId);
        onHandoffToReview?.(recipeId);
      });
    }
  };

  const handleKeepTimers = () => {
    setIsExitDialogOpen(false);
    runtime.exitSession(true);
    onClose?.();
  };

  const handleDiscardAll = () => {
    setIsExitDialogOpen(false);
    runtime.exitSession(false);
    onClose?.();
  };

  const toggleHighContrast = () => {
    const nextTheme = isHighContrast ? 'LIGHT' : 'HIGH_CONTRAST_DARK';
    runtime.setTheme(nextTheme);
  };

  const cycleFontScale = () => {
    const scales = [1, 1.25, 1.5, 2];
    const currentIdx = scales.indexOf(session.preferences.fontScale);
    const nextScale = scales[(currentIdx + 1) % scales.length];
    runtime.setFontScale(nextScale);
  };

  const toggleFullscreen = () => {
    if (fullscreenAdapter.isFullscreen()) {
      fullscreenAdapter.exit();
    } else if (containerRef.current) {
      fullscreenAdapter.request(containerRef.current);
    }
  };

  return (
    <div
      ref={containerRef}
      onClickCapture={(event) => {
        if (expiredCount > 0) {
          event.preventDefault();
          event.stopPropagation();
          handleSilenceAllAlarms();
        }
      }}
      data-testid="cooking-view-container"
      className={`relative flex min-h-screen flex-col font-sans transition-colors duration-200 ${
        isHighContrast
          ? 'theme-high-contrast-dark bg-black text-yellow-300'
          : 'bg-stone-50 text-stone-900'
      } ${expiredCount > 0 ? 'ring-4 ring-inset ring-red-500' : ''}`}
    >
      {/* Top Bar */}
      <header
        className={`flex items-center justify-between border-b px-4 py-3 sm:px-6 ${
          isHighContrast ? 'border-yellow-500/40 bg-black' : 'border-stone-200 bg-white shadow-sm'
        }`}
      >
        <button
          type="button"
          onClick={handleExitClick}
          aria-label={COOKING_VI.exitCooking}
          className={`flex items-center gap-1.5 rounded-lg px-3 py-2 text-sm font-semibold transition active:scale-95 ${
            isHighContrast
              ? 'text-yellow-400 hover:bg-yellow-950/40'
              : 'text-stone-600 hover:bg-stone-100'
          }`}
        >
          <span>✕</span>
          <span>{COOKING_VI.exitCooking}</span>
        </button>

        <div className="flex items-center gap-2">
          <h1
            ref={headingRef}
            tabIndex={-1}
            className="text-base font-bold sm:text-lg focus:outline-none"
          >
            {COOKING_VI.stepCounter(currentStepIndex + 1, totalSteps)}
          </h1>
          {wakeLockActive && (
            <span
              title={COOKING_VI.autoWakeActive}
              className="text-xs text-amber-500 hidden sm:inline"
              aria-label={COOKING_VI.autoWakeActive}
            >
              🔆
            </span>
          )}
        </div>

        <div className="flex items-center gap-2">
          {/* Push-to-Talk Voice Command Mic */}
          <button
            type="button"
            aria-label={isListening ? COOKING_VI.listeningActive : COOKING_VI.tapToSpeak}
            title={isListening ? COOKING_VI.listeningActive : COOKING_VI.tapToSpeak}
            onClick={toggleVoiceListening}
            className={`flex items-center gap-1.5 rounded-lg p-2 text-sm font-medium transition ${
              isListening
                ? 'animate-pulse bg-red-600 text-white'
                : isHighContrast
                ? 'bg-yellow-950 text-yellow-400 hover:bg-yellow-900'
                : 'bg-stone-100 text-stone-700 hover:bg-stone-200'
            }`}
          >
            <span>{isListening ? '🎙️' : '🎤'}</span>
            {isListening && (
              <span className="text-xs font-bold hidden sm:inline">
                {COOKING_VI.listeningActive}
              </span>
            )}
          </button>

          {/* Scalable Font Toggle (FR-006) */}
          <button
            type="button"
            aria-label="Cỡ chữ"
            title="Cỡ chữ"
            onClick={cycleFontScale}
            className={`rounded-lg px-2.5 py-2 text-xs font-bold transition ${
              isHighContrast
                ? 'bg-yellow-950 text-yellow-400 hover:bg-yellow-900'
                : 'bg-stone-100 text-stone-700 hover:bg-stone-200'
            }`}
          >
            A+
          </button>

          {/* High-contrast Theme Toggle */}
          <button
            type="button"
            aria-label={isHighContrast ? COOKING_VI.themeLight : COOKING_VI.themeHighContrastDark}
            title={isHighContrast ? COOKING_VI.themeLight : COOKING_VI.themeHighContrastDark}
            onClick={toggleHighContrast}
            className={`rounded-lg p-2 text-sm font-medium transition ${
              isHighContrast
                ? 'bg-yellow-400 text-black hover:bg-yellow-300'
                : 'bg-stone-100 text-stone-700 hover:bg-stone-200'
            }`}
          >
            {isHighContrast ? '☀️' : '🌓'}
          </button>

          {/* Fullscreen Toggle */}
          <button
            type="button"
            aria-label="Toàn màn hình"
            title="Toàn màn hình"
            onClick={toggleFullscreen}
            className={`rounded-lg p-2 text-sm font-medium transition ${
              isHighContrast
                ? 'bg-yellow-950 text-yellow-400 hover:bg-yellow-900'
                : 'bg-stone-100 text-stone-700 hover:bg-stone-200'
            }`}
          >
            ⛶
          </button>
        </div>
      </header>

      {/* Active Alarm Tap-to-Silence Banner (FR-014, US3-AC6, Edge Case 5) */}
      {expiredCount > 0 && (
        <button
          type="button"
          onClick={handleSilenceAllAlarms}
          className="w-full animate-pulse bg-red-600 px-4 py-3 text-center text-sm sm:text-base font-extrabold text-white shadow-lg hover:bg-red-700 transition"
        >
          <span>🔔</span>{' '}
          <span>
            {expiredCount > 1
              ? COOKING_VI.multipleTimersExpired(expiredCount)
              : COOKING_VI.tapToSilence}
          </span>
        </button>
      )}

      {/* Completion Banner */}
      {showCompletionBanner && (
        <div className="bg-emerald-600 px-4 py-3 text-center text-sm sm:text-base font-bold text-white shadow-md">
          🎉 {COOKING_VI.completionBanner}
        </div>
      )}

      {/* Voice Recognition Notification Banner */}
      {voiceNotice && (
        <div className="bg-purple-100 px-4 py-2.5 text-center text-xs sm:text-sm font-semibold text-purple-900 dark:bg-purple-950 dark:text-purple-200">
          <span>🎙️</span> <span>{voiceNotice}</span>
        </div>
      )}

      {/* Status Warning Banners */}
      {!wakeLockSupported && (
        <div className="bg-amber-100 px-4 py-2.5 text-center text-xs sm:text-sm font-medium text-amber-900 dark:bg-amber-950 dark:text-amber-200">
          <span>⚠️</span> <span>{COOKING_VI.unsupportedWakeLockNotice}</span>
        </div>
      )}

      {hasStorageWarning && (
        <div className="flex items-center justify-between bg-red-100 px-4 py-2 text-xs font-semibold text-red-900 dark:bg-red-950 dark:text-red-200">
          <span>{COOKING_VI.storageQuotaWarning}</span>
          <button type="button" onClick={clearWarnings} className="underline ml-2">✕</button>
        </div>
      )}

      {hasTabConflict && (
        <div className="flex items-center justify-between bg-amber-200 px-4 py-2.5 text-xs font-bold text-amber-950">
          <span>⚠️ {COOKING_VI.conflictWarning}</span>
          <button type="button" onClick={clearWarnings} className="underline ml-2">✕</button>
        </div>
      )}

      {hasClockJumpWarning && (
        <div className="flex items-center justify-between bg-blue-100 px-4 py-2 text-xs font-medium text-blue-900 dark:bg-blue-950 dark:text-blue-200">
          <span>ℹ️ {COOKING_VI.clockJumpWarning}</span>
          <button type="button" onClick={clearWarnings} className="underline ml-2">✕</button>
        </div>
      )}

      {/* Main Instruction Display */}
      <main className="flex flex-1 flex-col justify-center px-6 py-6 sm:px-12 md:py-10">
        <div className="mx-auto w-full max-w-4xl text-center">
          {currentStep.images && currentStep.images.length > 0 && (
            <div className="mb-6 flex flex-wrap justify-center gap-4">
              {currentStep.images.map((img, idx) => {
                const imgKey = `${currentStep.stepId}_${idx}_${img.url}`;
                if (failedImages[imgKey]) {
                  return (
                    <div
                      key={imgKey}
                      role="img"
                      aria-label={img.alt || COOKING_VI.stepImageAlt(currentStep.stepNumber)}
                      className="flex h-40 w-60 items-center justify-center rounded-2xl border border-dashed border-stone-300 bg-stone-100 px-4 text-xs text-stone-500 dark:border-stone-700 dark:bg-stone-900 dark:text-stone-400"
                    >
                      🖼️ {img.alt || COOKING_VI.stepImageAlt(currentStep.stepNumber)}
                    </div>
                  );
                }

                return (
                  <img
                    key={imgKey}
                    src={img.url}
                    alt={img.alt || COOKING_VI.stepImageAlt(currentStep.stepNumber)}
                    onError={() =>
                      setFailedImages((prev) => ({
                        ...prev,
                        [imgKey]: true,
                      }))
                    }
                    className="max-h-64 sm:max-h-80 w-auto rounded-2xl object-cover shadow-lg"
                  />
                );
              })}
            </div>
          )}

          <p
            className={`text-2xl sm:text-3xl md:text-4xl font-medium leading-relaxed tracking-wide ${
              isHighContrast ? 'text-yellow-200' : 'text-stone-800'
            }`}
            style={{ fontSize: `${session.preferences.fontScale * 1.75}rem` }}
          >
            {currentStep.instructionText}
          </p>

          {/* Embedded Step-Specific Ingredients Subsection (FR-009, US2-AC5) */}
          {stepMappedIngredients.length > 0 && (
            <div
              data-testid="step-embedded-ingredients"
              className={`mx-auto mt-5 max-w-xl rounded-xl border p-3.5 text-left ${
                isHighContrast
                  ? 'border-yellow-500/40 bg-yellow-950/20 text-yellow-200'
                  : 'border-amber-200 bg-amber-50/60 text-stone-800'
              }`}
            >
              <div className="mb-2 flex items-center justify-between">
                <span className="text-xs font-bold uppercase tracking-wider opacity-80">
                  🥗 {COOKING_VI.stepIngredients}
                </span>
                <button
                  type="button"
                  onClick={() => setIsIngredientDrawerOpen(true)}
                  className="text-xs font-semibold underline opacity-80 hover:opacity-100"
                >
                  {COOKING_VI.allIngredients}
                </button>
              </div>
              <div className="flex flex-wrap gap-2">
                {stepMappedIngredients.map((ing) => {
                  const isChecked = session.checkedIngredientRowIds.includes(ing.rowId);
                  const qtyDisplay = [ing.quantity, ing.unitName].filter(Boolean).join(' ');
                  return (
                    <button
                      key={ing.rowId}
                      type="button"
                      onClick={() => runtime.toggleIngredient(ing.rowId)}
                      className={`inline-flex items-center gap-1.5 rounded-lg border px-2.5 py-1 text-xs font-semibold transition ${
                        isChecked
                          ? 'border-emerald-300 bg-emerald-50/70 line-through opacity-60 dark:border-emerald-800 dark:bg-emerald-950/40'
                          : isHighContrast
                          ? 'border-yellow-500/40 bg-black text-yellow-300'
                          : 'border-stone-200 bg-white text-stone-800 shadow-sm'
                      }`}
                    >
                      <span>{isChecked ? '✓' : '○'}</span>
                      <span>{ing.name}</span>
                      {qtyDisplay && <span className="opacity-75">({qtyDisplay})</span>}
                    </button>
                  );
                })}
              </div>
            </div>
          )}

          {/* Step Quick Timers */}
          <StepQuickTimers
            step={currentStep}
            onStartQuickTimer={(seconds, label) => {
              primeAudio();
              runtime.addTimer(label, seconds, currentStep.stepNumber);
            }}
          />
        </div>

        {/* Active Timers Drawer */}
        <div className="mx-auto mt-6 w-full max-w-2xl">
          <TimerDrawer
            timers={timers}
            onPauseTimer={(id) => runtime.pauseTimer(id)}
            onResumeTimer={(id) => runtime.resumeTimer(id)}
            onExtendTimer={(id, sec) => runtime.extendTimer(id, sec)}
            onAcknowledgeTimer={(id) => runtime.acknowledgeTimer(id)}
            onDeleteTimer={(id) => runtime.deleteTimer(id)}
            onOpenCustomTimer={() => setIsCustomTimerDialogOpen(true)}
          />
        </div>

        {/* Embedded children */}
        {children && <div className="mt-6">{children}</div>}
      </main>

      {/* Oversized Step Navigation Bottom Bar */}
      <footer
        className={`border-t ${
          isHighContrast ? 'border-yellow-500/40 bg-black' : 'border-stone-200 bg-white'
        }`}
      >
        <StepNavigation
          currentStepIndex={currentStepIndex}
          totalSteps={totalSteps}
          onNext={() => runtime.nextStep()}
          onPrev={() => runtime.prevStep()}
          onSelectStep={(idx) => runtime.setStep(idx)}
          onFinish={handleFinishClick}
          onOpenIngredients={() => setIsIngredientDrawerOpen(true)}
          hasActiveAlarm={expiredCount > 0}
          onSilenceAlarm={handleSilenceAllAlarms}
        />
      </footer>

      {/* Ingredient Drawer */}
      <IngredientDrawer
        isOpen={isIngredientDrawerOpen}
        onClose={() => setIsIngredientDrawerOpen(false)}
        step={currentStep}
        allIngredients={snapshot.ingredients}
        checkedRowIds={session.checkedIngredientRowIds}
        onToggleIngredient={(rowId) => runtime.toggleIngredient(rowId)}
      />

      {/* Custom Timer Dialog */}
      <CustomTimerDialog
        isOpen={isCustomTimerDialogOpen}
        onClose={() => setIsCustomTimerDialogOpen(false)}
        onStartTimer={({ label, durationSeconds }) => {
          primeAudio();
          runtime.addTimer(label, durationSeconds, currentStep.stepNumber);
        }}
      />

      {/* Exit Session Confirmation Dialog */}
      <ExitSessionDialog
        isOpen={isExitDialogOpen}
        runningCount={runningCount}
        pausedCount={pausedCount}
        expiredCount={expiredCount}
        onKeepTimers={handleKeepTimers}
        onDiscardAll={handleDiscardAll}
        onCancel={() => setIsExitDialogOpen(false)}
      />

      {/* Resume Session Dialog */}
      <ResumeSessionDialog
        isOpen={isResumeDialogOpen}
        stepNumber={(resumableEnvelope?.session.currentStepIndex ?? 0) + 1}
        onResume={handleResumeConfirmed}
        onStartOver={handleStartOverConfirmed}
      />
    </div>
  );
}
