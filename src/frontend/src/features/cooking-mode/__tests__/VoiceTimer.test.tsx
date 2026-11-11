import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { act, fireEvent, render, screen, within } from '@testing-library/react';
import { parseVoiceTimer } from '../domain/voiceTimerParser';
import { CookingView } from '../components/CookingView';
import { CookingRuntimeProvider } from '../runtime/CookingRuntimeProvider';
import { CookingRuntimeManager } from '../runtime/cookingRuntime';
import { sharedSpeechAdapter } from '../adapters/speechAdapter';
import { COOKING_VI } from '../i18n/vi';
import { mockPhoBoSnapshot } from './fixtures/recipeFixtures';

describe('voice timer parsing', () => {
  it.each([
    ['hẹn giờ 5 phút luộc trứng', 300, 'luộc trứng'],
    ['hẹn giờ năm phút luộc trứng', 300, 'luộc trứng'],
    ['đặt hẹn giờ một giờ ba mươi phút nấu cháo', 5400, 'nấu cháo'],
    ['hẹn giờ 30 giây', 30, ''],
    ['hẹn giờ 5 phút.', 300, ''],
    ['hẹn giờ 5 phút, luộc trứng.', 300, 'luộc trứng'],
  ])('parses %s', (text, durationSeconds, label) => {
    expect(parseVoiceTimer(text)).toEqual({ durationSeconds, label });
  });
  it.each(['hẹn giờ', 'hẹn giờ 0 phút', 'hẹn giờ 3-5 phút', 'hẹn giờ 999 giờ', 'hẹn giờ 5 phút hoặc 10 phút', 'hẹn giờ khoảng 5 phút', 'hẹn giờ -5 phút', 'hẹn giờ 1.5 phút', 'hẹn giờ 60 phút', 'hẹn giờ 5 phút đến 10 phút', 'hẹn giờ 5 phút hoặc lâu hơn', 'hẹn giờ 5 phút rưỡi', 'hẹn giờ 1 giờ rưỡi nấu cháo'])('rejects ambiguous or invalid %s', (text) => {
    expect(parseVoiceTimer(text)).toBeNull();
  });
});

describe('voice timer recognition integration', () => {
  let runtime: CookingRuntimeManager;
  let recognition: { onresult: ((event: any) => void) | null };
  beforeEach(() => {
    localStorage.clear();
    vi.stubGlobal('SpeechRecognition', class {
      onresult = null;
      constructor() { recognition = this; }
      start() {}
      stop() {}
    });
    runtime = new CookingRuntimeManager();
    runtime.startSession(mockPhoBoSnapshot);
    render(<CookingRuntimeProvider customRuntime={runtime}><CookingView /></CookingRuntimeProvider>);
    fireEvent.click(screen.getByLabelText(COOKING_VI.tapToSpeak));
  });
  afterEach(() => { sharedSpeechAdapter.cleanup(); runtime.cleanup(); vi.unstubAllGlobals(); vi.restoreAllMocks(); });
  function say(text: string, confidence = 0.9) {
    act(() => recognition.onresult?.({ resultIndex: 0, results: [{ isFinal: true, 0: { transcript: text, confidence } }] }));
  }
  it('prefills without creating a timer, then starts on confirmation', () => {
    const add = vi.spyOn(runtime, 'addTimer');
    say('hẹn giờ năm phút luộc trứng');
    const dialog = screen.getByRole('dialog');
    expect(within(dialog).getByText(/Kiểm tra thời gian/)).toBeDefined();
    expect(screen.getByLabelText(COOKING_VI.tapToSpeak)).toBeDefined();
    expect(within(dialog).getByLabelText(COOKING_VI.timerMinutes)).toHaveValue(5);
    expect(within(dialog).getByPlaceholderText(COOKING_VI.timerLabelPlaceholder)).toHaveValue('luộc trứng');
    expect(add).not.toHaveBeenCalled();
    fireEvent.click(within(dialog).getByRole('button', { name: COOKING_VI.startTimer }));
    expect(add).toHaveBeenCalledWith('luộc trứng', 300, 1);
  });
  it('cancel and legacy command reopen a blank dialog', () => {
    const add = vi.spyOn(runtime, 'addTimer');
    say('hẹn giờ 5 phút luộc trứng');
    fireEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: COOKING_VI.cancel }));
    fireEvent.click(screen.getByLabelText(COOKING_VI.tapToSpeak));
    say('hẹn giờ');
    expect(within(screen.getByRole('dialog')).getByLabelText(COOKING_VI.timerMinutes)).toHaveValue(0);
    expect(add).not.toHaveBeenCalled();
  });
  it('ignores speech while a manual dialog is open', () => {
    fireEvent.click(screen.getByText(COOKING_VI.addCustomTimer));
    say('tiếp theo');
    say('hẹn giờ 5 phút luộc trứng');
    expect(screen.getByRole('heading', { name: 'Bước 1 / 5' })).toBeDefined();
    expect(within(screen.getByRole('dialog')).getByLabelText(COOKING_VI.timerMinutes)).toHaveValue(0);
  });
  it('stops before processing subsequent batched speech results', () => {
    act(() => recognition.onresult?.({ resultIndex: 0, results: [
      { isFinal: true, 0: { transcript: 'hẹn giờ 5 phút', confidence: 0.9 } },
      { isFinal: true, 0: { transcript: 'tiếp theo', confidence: 0.9 } },
    ] }));
    expect(screen.getByRole('heading', { name: 'Bước 1 / 5' })).toBeDefined();
    expect(screen.getByRole('dialog')).toBeDefined();
  });
  it('ignores low confidence and opens blank for ambiguous input', () => {
    say('hẹn giờ 5 phút luộc trứng', 0.6);
    expect(screen.queryByRole('dialog')).toBeNull();
    say('hẹn giờ 3-5 phút');
    expect(within(screen.getByRole('dialog')).getByText(/Vui lòng nhập thủ công/)).toBeDefined();
    expect(within(screen.getByRole('dialog')).getByLabelText(COOKING_VI.timerMinutes)).toHaveValue(0);
  });
});
