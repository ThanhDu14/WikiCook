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

    afterEach(async () => {
      await wakeLockAdapter.release();
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

    function sentinel() {
      const listeners = new Set<() => void>();
      return {
        released: false,
        release: vi.fn().mockResolvedValue(undefined),
        addEventListener: vi.fn((_type: 'release', listener: () => void) => listeners.add(listener)),
        removeEventListener: vi.fn((_type: 'release', listener: () => void) => listeners.delete(listener)),
        osRelease() {
          this.released = true;
          listeners.forEach(listener => listener());
        },
      };
    }

    function installRequest(request: ReturnType<typeof vi.fn>) {
      Object.defineProperty(global, 'navigator', {
        value: { wakeLock: { request } }, configurable: true, writable: true,
      });
    }

    it('releases a sentinel that resolves after release without announcing it active', async () => {
      const lock = sentinel();
      let resolve!: (value: typeof lock) => void;
      installRequest(vi.fn(() => new Promise<typeof lock>(done => { resolve = done; })));
      const listener = vi.fn();
      const unsubscribe = wakeLockAdapter.subscribe(listener);
      try {
        const pending = wakeLockAdapter.request();
        await wakeLockAdapter.release();
        resolve(lock);
        expect(await pending).toBeNull();
        expect(lock.release).toHaveBeenCalledTimes(1);
        expect(listener).not.toHaveBeenCalledWith(true);
      } finally { unsubscribe(); }
    });

    it('notifies subscribers of OS release without automatically reacquiring', async () => {
      const lock = sentinel();
      const request = vi.fn().mockResolvedValue(lock);
      installRequest(request);
      const listener = vi.fn();
      const unsubscribe = wakeLockAdapter.subscribe(listener);
      await wakeLockAdapter.request();
      expect(listener).toHaveBeenLastCalledWith(true);
      lock.osRelease();
      expect(listener).toHaveBeenLastCalledWith(false);
      expect(lock.removeEventListener).toHaveBeenCalledWith('release', expect.any(Function));
      expect(request).toHaveBeenCalledTimes(1);
      unsubscribe();
      listener.mockClear();
      await wakeLockAdapter.request();
      expect(listener).not.toHaveBeenCalled();
    });

    it('shares concurrent requests and reuses the active sentinel', async () => {
      const lock = sentinel();
      const request = vi.fn().mockResolvedValue(lock);
      installRequest(request);
      expect(await Promise.all([wakeLockAdapter.request(), wakeLockAdapter.request()])).toEqual([lock, lock]);
      expect(await wakeLockAdapter.request()).toBe(lock);
      expect(request).toHaveBeenCalledTimes(1);
      await wakeLockAdapter.release();
      expect(lock.release).toHaveBeenCalledTimes(1);
      expect(lock.removeEventListener).toHaveBeenCalled();
    });

    it('does not let a cancelled request overwrite a newer lock', async () => {
      const late = sentinel();
      const current = sentinel();
      let resolve!: (value: typeof late) => void;
      installRequest(vi.fn()
        .mockImplementationOnce(() => new Promise<typeof late>(done => { resolve = done; }))
        .mockResolvedValue(current));
      const pending = wakeLockAdapter.request();
      await wakeLockAdapter.release();
      expect(await wakeLockAdapter.request()).toBe(current);
      resolve(late);
      expect(await pending).toBeNull();
      expect(late.release).toHaveBeenCalledTimes(1);
      await wakeLockAdapter.release();
      expect(current.release).toHaveBeenCalledTimes(1);
    });

    it('does not clear a newer lock when an older release finishes', async () => {
      const old = sentinel();
      const current = sentinel();
      let finish!: () => void;
      old.release.mockImplementation(() => new Promise<void>(resolve => { finish = resolve; }));
      installRequest(vi.fn().mockResolvedValueOnce(old).mockResolvedValue(current));
      await wakeLockAdapter.request();
      const releasing = wakeLockAdapter.release();
      await wakeLockAdapter.request();
      finish();
      await releasing;
      await wakeLockAdapter.release();
      expect(current.release).toHaveBeenCalledTimes(1);
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
