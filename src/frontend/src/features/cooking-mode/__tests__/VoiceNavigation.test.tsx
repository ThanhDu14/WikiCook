import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, act } from '@testing-library/react';
import { SpeechAdapter, sharedSpeechAdapter } from '../adapters/speechAdapter';
import { COOKING_VI } from '../i18n/vi';
import { CookingView } from '../components/CookingView';
import { CookingRuntimeProvider } from '../runtime/CookingRuntimeProvider';
import { CookingRuntimeManager } from '../runtime/cookingRuntime';
import { mockPhoBoSnapshot } from './fixtures/recipeFixtures';

beforeEach(() => {
  vi.stubGlobal('SpeechRecognition', class {
    start() {}
    stop() {}
    abort() {}
  });
});
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); vi.useRealTimers(); });

describe('VoiceNavigation & speechAdapter (T037 - US4)', () => {
  let adapter: SpeechAdapter;

  beforeEach(() => {
    adapter = new SpeechAdapter();
  });

  afterEach(() => {
    adapter.cleanup();
  });

  it('recognizes 5 standard Vietnamese commands when confidence >= 0.70', () => {
    expect(adapter.classifyCommand('tiếp theo', 0.85)).toBe('NEXT_STEP');
    expect(adapter.classifyCommand('bước tiếp', 0.90)).toBe('NEXT_STEP');
    expect(adapter.classifyCommand('quay lại', 0.75)).toBe('PREV_STEP');
    expect(adapter.classifyCommand('bước trước', 0.80)).toBe('PREV_STEP');
    expect(adapter.classifyCommand('đọc lại', 0.95)).toBe('READ_STEP');
    expect(adapter.classifyCommand('hẹn giờ', 0.85)).toBe('OPEN_TIMER');
    expect(adapter.classifyCommand('tắt chuông', 0.80)).toBe('SILENCE_ALARM');
  });

  it('rejects commands with confidence < 0.70 per FR-016', () => {
    // Low confidence noise
    expect(adapter.classifyCommand('tiếp theo', 0.65)).toBe('UNKNOWN');
    expect(adapter.classifyCommand('tiếng ồn nhà bếp', 0.40)).toBe('UNKNOWN');
  });

  it('rejects unknown phrases even with high confidence', () => {
    expect(adapter.classifyCommand('bật tivi lên', 0.95)).toBe('UNKNOWN');
    expect(adapter.classifyCommand('hôm nay ăn gì', 0.90)).toBe('UNKNOWN');
  });

  it('stops listening after 30 seconds of idle timeout', () => {
    vi.useFakeTimers();
    const handleTimeout = vi.fn();

    adapter.startListening({ onIdleTimeout: handleTimeout });
    expect(adapter.isListening()).toBe(true);

    vi.advanceTimersByTime(30000);
    expect(handleTimeout).toHaveBeenCalled();
    expect(adapter.isListening()).toBe(false);

    vi.useRealTimers();
  });

  it('notifies network offline when attempting voice navigation offline', () => {
    const originalOnLine = navigator.onLine;
    Object.defineProperty(navigator, 'onLine', {
      value: false,
      configurable: true,
      writable: true,
    });

    try {
      const handleOffline = vi.fn();
      adapter.startListening({ onOffline: handleOffline });

      expect(handleOffline).toHaveBeenCalledWith(COOKING_VI.voiceOfflineNotice);
    } finally {
      Object.defineProperty(navigator, 'onLine', {
        value: originalOnLine,
        configurable: true,
        writable: true,
      });
    }
  });
});

describe('VoiceNavigation UI Integration in CookingView (T039 & T040 - US4)', () => {
  let runtime: CookingRuntimeManager;

  beforeEach(() => {
    localStorage.clear();
    runtime = new CookingRuntimeManager();
    runtime.startSession(mockPhoBoSnapshot);
  });

  afterEach(() => {
    runtime.cleanup();
    sharedSpeechAdapter.cleanup();
  });

  it('renders Push-to-Talk microphone button and toggles listening state', () => {
    render(
      <CookingRuntimeProvider customRuntime={runtime}>
        <CookingView />
      </CookingRuntimeProvider>,
    );

    const micBtn = screen.getByLabelText(COOKING_VI.tapToSpeak);
    expect(micBtn).toBeDefined();

    fireEvent.click(micBtn);

    // After clicking, button reflects listening state
    expect(screen.getByLabelText(COOKING_VI.listeningActive)).toBeDefined();
  });

  it('handles NEXT_STEP voice command dispatch and advances step', () => {
    let capturedCallbacks: Parameters<typeof sharedSpeechAdapter.startListening>[0];
    vi.spyOn(sharedSpeechAdapter, 'startListening').mockImplementation((cbs) => {
      capturedCallbacks = cbs;
      return true;
    });

    render(
      <CookingRuntimeProvider customRuntime={runtime}>
        <CookingView />
      </CookingRuntimeProvider>,
    );

    const micBtn = screen.getByLabelText(COOKING_VI.tapToSpeak);
    fireEvent.click(micBtn);

    expect(capturedCallbacks).toBeDefined();

    // Initial step is 1
    expect(screen.getByRole('heading', { name: 'Bước 1 / 5' })).toBeDefined();

    // Simulate voice recognized "tiếp theo"
    act(() => {
      capturedCallbacks?.onCommand?.('NEXT_STEP', 'tiếp theo');
    });

    // Step should advance to 2
    expect(screen.getByRole('heading', { name: 'Bước 2 / 5' })).toBeDefined();
  });

  it('displays "Không rõ khẩu lệnh" banner when command is UNKNOWN', () => {
    let capturedCallbacks: Parameters<typeof sharedSpeechAdapter.startListening>[0];
    vi.spyOn(sharedSpeechAdapter, 'startListening').mockImplementation((cbs) => {
      capturedCallbacks = cbs;
      return true;
    });

    render(
      <CookingRuntimeProvider customRuntime={runtime}>
        <CookingView />
      </CookingRuntimeProvider>,
    );

    const micBtn = screen.getByLabelText(COOKING_VI.tapToSpeak);
    fireEvent.click(micBtn);

    act(() => {
      capturedCallbacks?.onCommand?.('UNKNOWN', 'tiếng ồn');
    });

    expect(screen.getByText(COOKING_VI.unrecognizedCommand)).toBeDefined();
  });
});
