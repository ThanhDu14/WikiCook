// Deliberately limited grammar: uncertain durations are left for manual entry.
const digits: Record<string, number> = {
  không: 0, một: 1, hai: 2, ba: 3, bốn: 4, tư: 4, năm: 5,
  lăm: 5, sáu: 6, bảy: 7, tám: 8, chín: 9,
};

function numberValue(text: string): number | null {
  if (/^\d+$/.test(text)) return Number(text);
  const words = text.split(' ');
  if (words.length === 1) return digits[words[0]] ?? (words[0] === 'mười' ? 10 : null);
  const ten = words.indexOf('mươi');
  if (words[0] === 'mười' && words.length === 2 && digits[words[1]] !== undefined) return 10 + digits[words[1]];
  if (ten === 1 && words.length <= 3 && digits[words[0]] > 1 && (words.length === 2 || digits[words[2]] !== undefined)) {
    return digits[words[0]] * 10 + (digits[words[2]] ?? 0);
  }
  return null;
}

export function parseVoiceTimer(transcript: string): { durationSeconds: number; label: string } | null {
  const text = transcript.trim().toLowerCase().replace(/[,.!?](?!\d)/g, ' ').replace(/\s+/g, ' ').trim();
  const prefix = /^(?:(?:đặt|bật) )?hẹn giờ(?: |$)/.exec(text);
  if (!prefix) return null;
  let rest = text.slice(prefix[0].length);
  let total = 0;
  let previousUnit = 3601;
  let count = 0;
  const part = /^([\d]+|(?:không|một|hai|ba|bốn|tư|năm|lăm|sáu|bảy|tám|chín|mười|mươi)(?: (?:không|một|hai|ba|bốn|tư|năm|lăm|sáu|bảy|tám|chín|mười|mươi)){0,2}) (giờ|phút|giây)(?= |$)/;
  while (rest) {
    const match = part.exec(rest);
    if (!match) break;
    const value = numberValue(match[1]);
    const unit = match[2] === 'giờ' ? 3600 : match[2] === 'phút' ? 60 : 1;
    if (value === null || !Number.isSafeInteger(value) || unit >= previousUnit || value > (unit === 3600 ? 23 : 59)) return null;
    total += value * unit;
    previousUnit = unit;
    count++;
    rest = rest.slice(match[0].length).trim();
  }
  // Do not interpret ranges, alternatives, extra durations or numeric fragments as labels.
  if (!count || total <= 0 || total > 86400 || /[\d\-–]/u.test(rest) || /(?:^|\s)(?:hoặc|đến|tới|giờ|phút|giây|rưỡi)(?=\s|$)/u.test(rest) || /(?:^|\s)(?:một|hai|ba|bốn|năm|sáu|bảy|tám|chín|mười)(?=\s|$)/u.test(rest)) return null;
  return { durationSeconds: total, label: rest };
}
