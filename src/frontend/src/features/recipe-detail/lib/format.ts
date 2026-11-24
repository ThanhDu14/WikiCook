const NUMBER = new Intl.NumberFormat('vi-VN', { maximumFractionDigits: 1 });

export const DIFFICULTY: Record<string, string> = { EASY: 'Dễ', MEDIUM: 'Vừa', HARD: 'Khó' };

/** "45 phút", "1 giờ 30 phút". */
export function minutesLabel(minutes: number): string {
  if (minutes < 60) return `${minutes} phút`;
  const hours = Math.floor(minutes / 60);
  const rest = minutes % 60;
  return rest ? `${hours} giờ ${rest} phút` : `${hours} giờ`;
}

/** A step timer chip: "15 phút", "1 phút 30 giây". */
export function timerLabel(seconds: number): string {
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  if (minutes === 0) return `${rest} giây`;
  return rest ? `${minutesLabel(minutes)} ${rest} giây` : minutesLabel(minutes);
}

export function ratingLabel(value: number): string {
  return NUMBER.format(value);
}

/** The YouTube player URL for a watch / short / youtu.be link, or null for anything else. */
export function youtubeEmbedUrl(url: string): string | null {
  try {
    const parsed = new URL(url);
    const host = parsed.hostname.replace(/^www\.|^m\./, '');
    let id: string | null = null;
    if (host === 'youtu.be') id = parsed.pathname.slice(1);
    else if (host === 'youtube.com' && parsed.pathname === '/watch') id = parsed.searchParams.get('v');
    else if (host === 'youtube.com' && /^\/(shorts|embed)\//.test(parsed.pathname)) id = parsed.pathname.split('/')[2];
    return id && /^[\w-]{6,20}$/.test(id) ? `https://www.youtube-nocookie.com/embed/${id}` : null;
  } catch {
    return null;
  }
}
