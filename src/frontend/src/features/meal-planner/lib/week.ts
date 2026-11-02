/**
 * Weeks of the planner follow the Vietnam calendar whatever the device zone (research R2, T027). Dates are
 * "YYYY-MM-DD" strings; arithmetic runs on UTC midnights so it never shifts by a zone offset.
 */

const VIETNAM = 'Asia/Ho_Chi_Minh';
const DAY_MS = 86_400_000;

const isoDate = new Intl.DateTimeFormat('en-CA', { timeZone: VIETNAM, year: 'numeric', month: '2-digit', day: '2-digit' });

function toUtc(date: string): number {
  const [y, m, d] = date.split('-').map(Number);
  return Date.UTC(y, m - 1, d);
}

function fromUtc(ms: number): string {
  return new Date(ms).toISOString().slice(0, 10);
}

/** Today's date in Vietnam. */
export function todayInVietnam(now: Date = new Date()): string {
  return isoDate.format(now);
}

export function addDays(date: string, days: number): string {
  return fromUtc(toUtc(date) + days * DAY_MS);
}

/** Monday of the week containing `date`. */
export function weekStartOf(date: string): string {
  const isoDay = (new Date(toUtc(date)).getUTCDay() + 6) % 7; // 0 = Monday
  return addDays(date, -isoDay);
}

export function currentWeekStart(now: Date = new Date()): string {
  return weekStartOf(todayInVietnam(now));
}

export function addWeeks(weekStart: string, weeks: number): string {
  return addDays(weekStart, weeks * 7);
}

/** Date of day `index` (0 = Monday … 6 = Sunday) of the week. */
export function dateOf(weekStart: string, index: number): string {
  return addDays(weekStart, index);
}

export function isMonday(date: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date) || Number.isNaN(toUtc(date))) return false;
  return fromUtc(toUtc(date)) === date && weekStartOf(date) === date;
}

/** Index of today in the week, or -1 when the week does not contain today. */
export function dayIndexOfToday(weekStart: string, now: Date = new Date()): number {
  const index = Math.round((toUtc(todayInVietnam(now)) - toUtc(weekStart)) / DAY_MS);
  return index >= 0 && index < 7 ? index : -1;
}

const pad = (n: number) => String(n).padStart(2, '0');

/** "dd/mm" of a date, with "/yyyy" when asked. */
export function formatDate(date: string, withYear = false): string {
  const [y, m, d] = date.split('-');
  return withYear ? `${d}/${m}/${y}` : `${d}/${m}`;
}

/** "13/10 – 19/10/2026"; the year shows on both ends when the week crosses it. */
export function formatWeekRange(weekStart: string): string {
  const end = addDays(weekStart, 6);
  const sameYear = weekStart.slice(0, 4) === end.slice(0, 4);
  return `${formatDate(weekStart, !sameYear)} – ${formatDate(end, true)}`;
}

/** Day of the month, two digits. */
export function dayOfMonth(date: string): string {
  return pad(Number(date.slice(8, 10)));
}

/** Title of a week as in W-MP01, "27/10 – 02/11"; the years show when the week crosses one. */
export function formatWeekTitle(weekStart: string): string {
  const end = addDays(weekStart, 6);
  const sameYear = weekStart.slice(0, 4) === end.slice(0, 4);
  return `${formatDate(weekStart, !sameYear)} – ${formatDate(end, !sameYear)}`;
}
