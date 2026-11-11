import { describe, it, expect, vi, afterEach } from 'vitest';
import {
  wakeLockAdapter,
  fullscreenAdapter,
  vibrationAdapter,
  getAudioSynth,
  resetAudioSynthForTesting,
} from '../adapters/browserCapabilities';

describe('browserAdapters (T008)', () => {
  describe('WakeLockAdapter', () => {
    const originalNavigator = global.navigator;

    afterEach(() => {
      Object.defineProperty(global, 'navigator', {
        value: originalNavigator,
        configurable: true,
        writable: true,
      });
    });

    it('detects unsupported Wake Lock gracefully', () => {
      Object.defineProperty(global, 'navigator', {
        value: {},
        configurable: true,
        writable: true,
      });

      expect(wakeLockAdapter.isSupported()).toBe(false);
    });

    it('requests and releases Wake Lock when supported', async () => {
      const releaseMock = vi.fn().mockResolvedValue(undefined);
      const addEventListenerMock = vi.fn();
      const removeEventListenerMock = vi.fn();
      const mockSentinel = {
        released: false,
        release: releaseMock,
        addEventListener: addEventListenerMock,
        removeEventListener: removeEventListenerMock,
      };

      const requestMock = vi.fn().mockResolvedValue(mockSentinel);

      Object.defineProperty(global, 'navigator', {
        value: {
          wakeLock: {
            request: requestMock,
          },
        },
        configurable: true,
        writable: true,
      });

      expect(wakeLockAdapter.isSupported()).toBe(true);
      const lock = await wakeLockAdapter.request();
      expect(requestMock).toHaveBeenCalledWith('screen');
      expect(lock).toBe(mockSentinel);

      await wakeLockAdapter.release();
      expect(releaseMock).toHaveBeenCalled();
    });

    it('returns null and does not throw when request fails', async () => {
      Object.defineProperty(global, 'navigator', {
        value: {
          wakeLock: {
            request: vi.fn().mockRejectedValue(new Error('Permission denied')),
          },
        },
        configurable: true,
        writable: true,
      });

      const lock = await wakeLockAdapter.request();
      expect(lock).toBeNull();
    });
  });

  describe('FullscreenAdapter', () => {
    it('detects fullscreen capability and state', async () => {
      const requestMock = vi.fn().mockResolvedValue(undefined);
      const element = document.createElement('div');
      element.requestFullscreen = requestMock;

      const success = await fullscreenAdapter.request(element);
      expect(requestMock).toHaveBeenCalled();
      expect(success).toBe(true);
    });

    it('handles request rejection gracefully', async () => {
      const element = document.createElement('div');
      element.requestFullscreen = vi.fn().mockRejectedValue(new Error('Blocked'));

      const success = await fullscreenAdapter.request(element);
      expect(success).toBe(false);
    });
  });

  describe('VibrationAdapter', () => {
    it('vibrates with pattern and cancels with 0', () => {
      const vibrateMock = vi.fn().mockReturnValue(true);
      Object.defineProperty(global.navigator, 'vibrate', {
        value: vibrateMock,
        configurable: true,
        writable: true,
      });

      expect(vibrationAdapter.isSupported()).toBe(true);
      const res = vibrationAdapter.vibrate([200, 100, 200]);
      expect(vibrateMock).toHaveBeenCalledWith([200, 100, 200]);
      expect(res).toBe(true);

      vibrationAdapter.cancel();
      expect(vibrateMock).toHaveBeenCalledWith(0);
    });
  });

  describe('AudioSynthController', () => {
    afterEach(() => {
      resetAudioSynthForTesting();
    });

    it('unlocks audio context and handles pulse / earcon safely', async () => {
      const synth = getAudioSynth();
      expect(synth).toBeDefined();

      // AudioContext might be mocked or fallback in jsdom
      const unlocked = await synth.unlock();
      // Should not throw in jsdom
      expect(typeof unlocked).toBe('boolean');

      expect(() => synth.playEarcon()).not.toThrow();
      expect(() => synth.playAlarmPulse()).not.toThrow();
      expect(() => synth.stopAlarm()).not.toThrow();
    });
  });
});
