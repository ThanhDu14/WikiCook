import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { SpeechAdapter } from '../adapters/speechAdapter';

class Recognition {
  static instances: Recognition[] = [];
  lang = '';
  continuous = false;
  interimResults = true;
  onresult: ((event: { resultIndex: number; results: { isFinal: boolean; 0: { transcript: string; confidence: number } }[] }) => void) | null = null;
  onerror: ((event: { error: string }) => void) | null = null;
  onend: (() => void) | null = null;
  start = vi.fn();
  stop = vi.fn();
  constructor() { Recognition.instances.push(this); }
  result(transcript = 'tiếp theo', confidence = 0.9, isFinal = true) {
    this.onresult?.({ resultIndex: 0, results: [{ isFinal, 0: { transcript, confidence } }] });
  }
}

describe('browser speech lifecycle', () => {
  let adapter: SpeechAdapter;
  const latest = () => Recognition.instances[Recognition.instances.length - 1];
  beforeEach(() => {
    vi.useFakeTimers();
    Recognition.instances = [];
    vi.stubGlobal('SpeechRecognition', Recognition);
    vi.stubGlobal('webkitSpeechRecognition', undefined);
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(true);
    adapter = new SpeechAdapter();
  });
  afterEach(() => {
    adapter.cleanup();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    vi.useRealTimers();
  });

  it('starts Vietnamese browser recognition and dispatches final results only', () => {
    const onCommand = vi.fn();
    expect(adapter.startListening({ onCommand })).toBe(true);
    expect(latest().start).toHaveBeenCalledOnce();
    expect(latest().lang).toBe('vi-VN');
    latest().result('tiếp theo', 0.9, false);
    expect(onCommand).not.toHaveBeenCalled();
    latest().result('tiếp theo', 0.69);
    expect(onCommand).toHaveBeenLastCalledWith('UNKNOWN', 'tiếp theo');
    latest().result('tiếp theo', 0.7);
    expect(onCommand).toHaveBeenLastCalledWith('NEXT_STEP', 'tiếp theo');
    latest().result('tiếp theo', NaN);
    expect(onCommand).toHaveBeenLastCalledWith('UNKNOWN', 'tiếp theo');
  });

  it('supports the webkit vendor constructor', () => {
    vi.stubGlobal('SpeechRecognition', undefined);
    vi.stubGlobal('webkitSpeechRecognition', Recognition);
    expect(adapter.startListening()).toBe(true);
    expect(latest().start).toHaveBeenCalledOnce();
  });

  it('returns false when unsupported or offline without opening the microphone', () => {
    vi.stubGlobal('SpeechRecognition', undefined);
    expect(adapter.startListening()).toBe(false);
    vi.stubGlobal('SpeechRecognition', Recognition);
    vi.spyOn(navigator, 'onLine', 'get').mockReturnValue(false);
    const onOffline = vi.fn();
    expect(adapter.startListening({ onOffline })).toBe(false);
    expect(onOffline).toHaveBeenCalledOnce();
    expect(Recognition.instances).toHaveLength(0);
  });

  it('returns false on synchronous permission denial', () => {
    vi.stubGlobal('SpeechRecognition', class extends Recognition {
      start = vi.fn(() => { throw new DOMException('Denied', 'NotAllowedError'); });
    });
    const onError = vi.fn();
    expect(adapter.startListening({ onError })).toBe(false);
    expect(adapter.isListening()).toBe(false);
    expect(onError).toHaveBeenCalledOnce();
  });

  it('handles asynchronous permission denial and refuses subsequent starts', () => {
    const onError = vi.fn();
    adapter.startListening({ onError });
    latest().onerror?.({ error: 'not-allowed' });
    expect(adapter.isListening()).toBe(false);
    expect(onError).toHaveBeenCalledOnce();
    expect(adapter.startListening()).toBe(false);
  });

  it('resets the 30 second idle timeout on final speech', () => {
    const onIdleTimeout = vi.fn();
    adapter.startListening({ onIdleTimeout });
    vi.advanceTimersByTime(20000);
    latest().result();
    vi.advanceTimersByTime(29999);
    expect(adapter.isListening()).toBe(true);
    vi.advanceTimersByTime(1);
    expect(adapter.isListening()).toBe(false);
    expect(latest().stop).toHaveBeenCalledOnce();
    expect(onIdleTimeout).toHaveBeenCalledOnce();
  });

  it('stops on network loss and reports offline', () => {
    const onOffline = vi.fn();
    adapter.startListening({ onOffline });
    window.dispatchEvent(new Event('offline'));
    expect(adapter.isListening()).toBe(false);
    expect(onOffline).toHaveBeenCalledOnce();
    expect(latest().stop).toHaveBeenCalledOnce();
  });

  it('cleans handlers, microphone and timeout and ignores stale callbacks', () => {
    const onCommand = vi.fn();
    const onIdleTimeout = vi.fn();
    adapter.startListening({ onCommand, onIdleTimeout });
    const recognition = latest();
    const staleResult = recognition.onresult;
    adapter.cleanup();
    expect(recognition.stop).toHaveBeenCalledOnce();
    expect(recognition.onresult).toBeNull();
    staleResult?.({ resultIndex: 0, results: [{ isFinal: true, 0: { transcript: 'tiếp theo', confidence: 1 } }] });
    vi.runAllTimers();
    expect(onCommand).not.toHaveBeenCalled();
    expect(onIdleTimeout).not.toHaveBeenCalled();
  });

  it('handles natural recognition end without leaving a live idle timer', () => {
    const onIdleTimeout = vi.fn();
    adapter.startListening({ onIdleTimeout });
    latest().onend?.();
    expect(adapter.isListening()).toBe(false);
    vi.runAllTimers();
    expect(onIdleTimeout).not.toHaveBeenCalled();
  });

  it('does not create duplicate recognition sessions', () => {
    adapter.startListening();
    adapter.startListening();
    expect(Recognition.instances).toHaveLength(1);
  });

  it('waits briefly for voiceschanged and cancels speech on cleanup', () => {
    const synth = new EventTarget();
    const voice = { lang: 'vi-VN' };
    const getVoices = vi.fn().mockReturnValue([]);
    const speak = vi.fn();
    const cancel = vi.fn();
    Object.assign(synth, { getVoices, speak, cancel });
    vi.stubGlobal('speechSynthesis', synth);
    vi.stubGlobal('SpeechSynthesisUtterance', class { constructor(public text: string) {} });
    const fallback = vi.fn();
    adapter.speakStep('Đun sôi', fallback);
    expect(fallback).not.toHaveBeenCalled();
    getVoices.mockReturnValue([voice]);
    synth.dispatchEvent(new Event('voiceschanged'));
    expect(speak).toHaveBeenCalledOnce();
    expect(speak.mock.calls[0][0]).toMatchObject({ text: 'Đun sôi', lang: 'vi-VN', voice });
    adapter.cleanup();
    expect(cancel).toHaveBeenCalled();
    vi.runAllTimers();
    expect(fallback).not.toHaveBeenCalled();
  });
});
