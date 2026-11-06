import { addWeeks, currentWeekStart, dateOf, dayIndexOfToday, formatWeekRange, isMonday, todayInVietnam } from '../lib/week';

// Instants are fixed in UTC, so the results hold whatever zone the machine runs in (T012).
describe('week (Asia/Ho_Chi_Minh)', () => {
  it('starts the week on Monday in Vietnam time', () => {
    expect(currentWeekStart(new Date('2026-10-21T05:00:00Z'))).toBe('2026-10-19');
    // Sunday 17:30 UTC is Monday 00:30 in Vietnam: already the new week.
    expect(currentWeekStart(new Date('2026-10-18T17:30:00Z'))).toBe('2026-10-19');
    // Sunday 16:59 UTC is still Sunday 23:59 in Vietnam.
    expect(currentWeekStart(new Date('2026-10-18T16:59:00Z'))).toBe('2026-10-12');
    expect(todayInVietnam(new Date('2026-10-18T17:30:00Z'))).toBe('2026-10-19');
  });

  it('adds weeks across months and years', () => {
    expect(addWeeks('2026-10-19', 1)).toBe('2026-10-26');
    expect(addWeeks('2026-10-19', -1)).toBe('2026-10-12');
    expect(addWeeks('2026-12-28', 1)).toBe('2027-01-04');
  });

  it('formats the week range in Vietnamese order', () => {
    expect(formatWeekRange('2026-10-13')).toBe('13/10 – 19/10/2026');
    expect(formatWeekRange('2026-12-28')).toBe('28/12/2026 – 03/01/2027');
  });

  it('knows the dates of a week and which day is today', () => {
    expect(dateOf('2026-12-28', 6)).toBe('2027-01-03');
    expect(dayIndexOfToday('2026-10-19', new Date('2026-10-21T05:00:00Z'))).toBe(2);
    expect(dayIndexOfToday('2026-10-26', new Date('2026-10-21T05:00:00Z'))).toBe(-1);
  });

  it('checks for Mondays', () => {
    expect(isMonday('2026-10-19')).toBe(true);
    expect(isMonday('2026-10-20')).toBe(false);
    expect(isMonday('not-a-date')).toBe(false);
  });
});
