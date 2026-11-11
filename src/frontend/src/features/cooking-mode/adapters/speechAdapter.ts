import { COOKING_VI } from '../i18n/vi';
import { getAudioSynth } from './browserCapabilities';

export type VoiceCommand =
  | 'NEXT_STEP'
  | 'PREV_STEP'
  | 'READ_STEP'
  | 'OPEN_TIMER'
  | 'SILENCE_ALARM'
  | 'UNKNOWN';

export interface SpeechAdapterCallbacks {
  onCommand?: (command: VoiceCommand, rawTranscript: string) => void;
  onError?: (errorMessage: string) => void;
  onOffline?: (message: string) => void;
  onIdleTimeout?: () => void;
  onStopped?: () => void;
  onUnsupportedTts?: (message: string) => void;
}

interface RecognitionResult {
  isFinal: boolean;
  readonly [index: number]: { transcript: string; confidence: number };
}

interface BrowserRecognition {
  lang: string;
  continuous: boolean;
  interimResults: boolean;
  onresult: ((event: { resultIndex: number; results: ArrayLike<RecognitionResult> }) => void) | null;
  onerror: ((event: { error: string }) => void) | null;
  onend: (() => void) | null;
  start(): void;
  stop(): void;
}

type RecognitionConstructor = new () => BrowserRecognition;

function getRecognitionConstructor(): RecognitionConstructor | undefined {
  if (typeof window === 'undefined') return undefined;
  const browser = window as unknown as {
    SpeechRecognition?: RecognitionConstructor;
    webkitSpeechRecognition?: RecognitionConstructor;
  };
  return browser.SpeechRecognition || browser.webkitSpeechRecognition;
}

export class SpeechAdapter {
  private listening: boolean = false;
  private idleTimeoutId: number | null = null;
  private callbacks: SpeechAdapterCallbacks = {};

  private recognition: BrowserRecognition | null = null;
  private permissionDenied = false;
  private clearPendingSpeech: (() => void) | null = null;
  private activeSynth: SpeechSynthesis | null = null;
  private readonly onOffline = (): void => {
    this.stopListening();
    this.callbacks.onOffline?.(COOKING_VI.voiceOfflineNotice);
  };

  public isSupported(): boolean {
    return !!getRecognitionConstructor();
  }

  public isListening(): boolean {
    return this.listening;
  }

  public classifyCommand(transcript: string, confidence: number): VoiceCommand {
    if (!Number.isFinite(confidence) || confidence < 0.7) {
      return 'UNKNOWN';
    }

    const normalized = transcript.trim().toLowerCase();

    // A timer request may contain words used by other commands in its activity label.
    if (/^(?:(?:đặt|bật) )?hẹn giờ(?:\s|$)/.test(normalized)) return 'OPEN_TIMER';

    // Whitelist matching of the 5 standard Vietnamese commands
    if (
      normalized.includes('tiếp theo') ||
      normalized === 'tiếp' ||
      normalized.includes('bước tiếp')
    ) {
      return 'NEXT_STEP';
    }

    if (
      normalized.includes('quay lại') ||
      normalized === 'lùi' ||
      normalized.includes('bước trước')
    ) {
      return 'PREV_STEP';
    }

    if (
      normalized.includes('đọc lại') ||
      normalized.includes('đọc bước') ||
      normalized === 'đọc'
    ) {
      return 'READ_STEP';
    }

    if (
      normalized.includes('hẹn giờ') ||
      normalized.includes('đặt hẹn giờ') ||
      normalized.includes('bật hẹn giờ')
    ) {
      return 'OPEN_TIMER';
    }

    if (
      normalized.includes('tắt chuông') ||
      normalized.includes('dừng chuông') ||
      normalized.includes('tắt báo')
    ) {
      return 'SILENCE_ALARM';
    }

    return 'UNKNOWN';
  }

  public startListening(callbacks: SpeechAdapterCallbacks = {}): boolean {
    this.callbacks = callbacks;

    if (typeof navigator !== 'undefined' && !navigator.onLine) {
      this.stopListening();
      this.callbacks.onOffline?.(COOKING_VI.voiceOfflineNotice);
      return false;
    }

    const Constructor = getRecognitionConstructor();
    if (!Constructor || this.permissionDenied) return false;
    if (this.listening) return true;

    try {
      const recognition = new Constructor();
      this.recognition = recognition;
      recognition.lang = 'vi-VN';
      recognition.continuous = true;
      recognition.interimResults = false;
      recognition.onresult = (event) => {
        if (this.recognition !== recognition || !this.listening) return;
        for (let index = event.resultIndex; index < event.results.length; index++) {
          const result = event.results[index];
          if (!result.isFinal || !result[0]) continue;
          this.resetIdleTimer();
          const { transcript, confidence } = result[0];
          this.callbacks.onCommand?.(this.classifyCommand(transcript, confidence), transcript);
          if (this.recognition !== recognition) break;
        }
      };
      recognition.onerror = ({ error }) => {
        if (this.recognition !== recognition) return;
        if (error === 'not-allowed' || error === 'service-not-allowed') {
          this.permissionDenied = true;
        }
        this.stopListening();
        if (error === 'network') this.callbacks.onOffline?.(COOKING_VI.voiceOfflineNotice);
        else this.callbacks.onError?.(error);
      };
      recognition.onend = () => {
        if (this.recognition === recognition) this.stopListening();
      };
      this.listening = true;
      window.addEventListener('offline', this.onOffline);
      this.resetIdleTimer();
      recognition.start();
      return this.listening;
    } catch (error) {
      if (error instanceof DOMException && error.name === 'NotAllowedError') {
        this.permissionDenied = true;
      }
      this.stopListening();
      this.callbacks.onError?.(error instanceof Error ? error.message : String(error));
      return false;
    }
  }

  public stopListening(): void {
    const wasListening = this.listening;
    this.listening = false;
    if (wasListening) this.callbacks.onStopped?.();
    this.clearIdleTimer();
    if (typeof window !== 'undefined') window.removeEventListener('offline', this.onOffline);
    const recognition = this.recognition;
    this.recognition = null;
    if (recognition) {
      recognition.onresult = null;
      recognition.onerror = null;
      recognition.onend = null;
      try { recognition.stop(); } catch { /* Already ended by the browser. */ }
    }
  }

  private resetIdleTimer(): void {
    this.clearIdleTimer();
    if (typeof window !== 'undefined') {
      this.idleTimeoutId = window.setTimeout(() => {
        this.stopListening();
        this.callbacks.onIdleTimeout?.();
      }, 30000);
    }
  }

  private clearIdleTimer(): void {
    if (this.idleTimeoutId !== null) {
      clearTimeout(this.idleTimeoutId);
      this.idleTimeoutId = null;
    }
  }

  public speakStep(text: string, onFallback?: () => void): void {
    this.clearPendingSpeech?.();
    this.activeSynth?.cancel();
    const fallback = (): void => {
      getAudioSynth().playEarcon();
      this.callbacks.onUnsupportedTts?.(COOKING_VI.unsupportedTtsNotice);
      onFallback?.();
    };
    if (typeof window === 'undefined' || !window.speechSynthesis) {
      fallback();
      return;
    }

    const synth = window.speechSynthesis;
    this.activeSynth = synth;
    try {
      const speak = (): boolean => {
        const voice = synth.getVoices().find((v) => v.lang.toLowerCase().startsWith('vi'));
        if (!voice) return false;
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.voice = voice;
        utterance.lang = 'vi-VN';
        utterance.rate = 1.0;
        synth.speak(utterance);
        return true;
      };
      if (speak()) return;
      if (synth.getVoices().length > 0) {
        fallback();
        return;
      }
      // Some browsers populate voices asynchronously after the first request.
      const clear = (): void => {
        window.clearTimeout(timeout);
        synth.removeEventListener('voiceschanged', onVoicesChanged);
        this.clearPendingSpeech = null;
      };
      const onVoicesChanged = (): void => {
        clear();
        try { if (!speak()) fallback(); } catch { fallback(); }
      };
      const timeout = window.setTimeout(() => {
        clear();
        fallback();
      }, 1000);
      this.clearPendingSpeech = clear;
      synth.addEventListener('voiceschanged', onVoicesChanged);
    } catch {
      this.clearPendingSpeech?.();
      fallback();
    }
  }

  public cleanup(): void {
    this.stopListening();
    this.clearPendingSpeech?.();
    this.activeSynth?.cancel();
    this.activeSynth = null;
    this.callbacks = {};
  }
}

export const sharedSpeechAdapter = new SpeechAdapter();
