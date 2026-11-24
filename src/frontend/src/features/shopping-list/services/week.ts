/** Weeks as the planner (030) addresses them: the Monday, as a local yyyy-MM-dd date. */

function pad(n: number): string {
  return String(n).padStart(2, '0');
}

export function toIsoDate(date: Date): string {
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
}

export function parseIsoDate(iso: string): Date | null {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(iso);
  if (!match) return null;
  const date = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
  return Number.isNaN(date.getTime()) ? null : date;
}

/** The Monday of the week holding `date`. */
export function mondayOf(date: Date): Date {
  const monday = new Date(date.getFullYear(), date.getMonth(), date.getDate());
  monday.setDate(monday.getDate() - ((monday.getDay() + 6) % 7));
  return monday;
}

export function addWeeks(weekStart: string, weeks: number): string {
  const date = parseIsoDate(weekStart) ?? mondayOf(new Date());
  date.setDate(date.getDate() + weeks * 7);
  return toIsoDate(date);
}

/** "23/11 – 29/11/2026", as in the planner's header. */
export function weekLabel(weekStart: string): string {
  const start = parseIsoDate(weekStart) ?? mondayOf(new Date());
  const end = new Date(start);
  end.setDate(end.getDate() + 6);
  return `${pad(start.getDate())}/${pad(start.getMonth() + 1)} – ${pad(end.getDate())}/${pad(end.getMonth() + 1)}/${end.getFullYear()}`;
}
