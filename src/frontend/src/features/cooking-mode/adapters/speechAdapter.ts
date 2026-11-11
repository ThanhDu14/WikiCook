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
  onUnsupportedTts?: (message: string) => void;
}

export class SpeechAdapter {
  private listening: boolean = false;
  private idleTimeoutId: number | null = null;
  private callbacks: SpeechAdapterCallbacks = {};

  public isSupported(): boolean {
    if (typeof window === 'undefined') return false;
    return !!(
      (window as unknown as { SpeechRecognition?: unknown }).SpeechRecognition ||
      (window as unknown as { webkitSpeechRecognition?: unknown }).webkitSpeechRecognition
    );
  }

  public isListening(): boolean {
    return this.listening;
  }

  public classifyCommand(transcript: string, confidence: number): VoiceCommand {
    if (confidence < 0.7) {
      return 'UNKNOWN';
    }

    const normalized = transcript.trim().toLowerCase();

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
      this.callbacks.onOffline?.(COOKING_VI.voiceOfflineNotice);
      return false;
    }

    this.listening = true;
    this.resetIdleTimer();

    return true;
  }

  public stopListening(): void {
    this.listening = false;
    this.clearIdleTimer();
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
    if (typeof window === 'undefined' || !('speechSynthesis' in window)) {
      getAudioSynth().playEarcon();
      this.callbacks.onUnsupportedTts?.(COOKING_VI.unsupportedTtsNotice);
      onFallback?.();
      return;
    }

    try {
      const synth = window.speechSynthesis;
      const voices = synth.getVoices ? synth.getVoices() : [];
      const viVoice = voices.find((v) => v.lang.toLowerCase().startsWith('vi'));

      if (!viVoice) {
        // Fallback earcon chime per FR-017
        getAudioSynth().playEarcon();
        this.callbacks.onUnsupportedTts?.(COOKING_VI.unsupportedTtsNotice);
        onFallback?.();
        return;
      }

      const utterance = new SpeechSynthesisUtterance(text);
      utterance.voice = viVoice;
      utterance.lang = 'vi-VN';
      utterance.rate = 1.0;

      synth.speak(utterance);
    } catch {
      getAudioSynth().playEarcon();
      onFallback?.();
    }
  }

  public cleanup(): void {
    this.stopListening();
    this.callbacks = {};
  }
}

export const sharedSpeechAdapter = new SpeechAdapter();
