import { strings } from '../strings.vi';
import type { RatingSummary, StarValue } from '../types';

/** "4,6": one decimal with the Vietnamese comma, as on the screens (FR-009). */
export const formatAverage = (value: number) => value.toFixed(1).replace('.', ',');

/** Share of `star` in the distribution, rounded to a whole percent. */
export function starPercent(summary: RatingSummary, star: StarValue): number {
  if (summary.totalReviews <= 0) return 0;
  return Math.round((summary.ratingDistribution[String(star) as '1'] / summary.totalReviews) * 100);
}

/** "NH" for "Ngọc Hiền": first letter of the first and the last word. */
export function initials(name: string): string {
  const words = name.trim().split(/\s+/).filter(Boolean);
  if (words.length === 0) return '?';
  const first = words[0][0];
  const last = words.length > 1 ? words[words.length - 1][0] : '';
  return (first + last).toUpperCase();
}

/** "2 ngày trước" relative to `now`. */
export function relativeTime(iso: string, now: Date = new Date()): string {
  const seconds = Math.max(0, (now.getTime() - new Date(iso).getTime()) / 1000);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);
  const days = Math.floor(hours / 24);
  if (minutes < 1) return strings.time.justNow;
  if (hours < 1) return strings.time.minutes(minutes);
  if (days < 1) return strings.time.hours(hours);
  if (days < 7) return strings.time.days(days);
  if (days < 30) return strings.time.weeks(Math.floor(days / 7));
  if (days < 365) return strings.time.months(Math.floor(days / 30));
  return strings.time.years(Math.floor(days / 365));
}
