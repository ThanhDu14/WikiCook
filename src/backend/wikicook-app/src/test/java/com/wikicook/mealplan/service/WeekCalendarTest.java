package com.wikicook.mealplan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

/** 030 T007: weeks start on Monday in Vietnam time, whatever zone the clock is in. */
class WeekCalendarTest {

    private static Clock at(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
    }

    @Test
    void todayIsTheVietnamDate() {
        // Sunday 17:30 UTC is Monday 00:30 in Vietnam: already the new week.
        Clock clock = at("2026-11-01T17:30:00Z");
        assertThat(WeekCalendar.today(clock)).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(WeekCalendar.currentWeekStart(clock)).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(WeekCalendar.currentWeekStart(at("2026-11-01T16:59:00Z"))).isEqualTo(LocalDate.of(2026, 10, 26));
    }

    @Test
    void weekStartOfAnyDayIsItsMonday() {
        assertThat(WeekCalendar.weekStartOf(LocalDate.of(2026, 10, 30))).isEqualTo(LocalDate.of(2026, 10, 26));
        assertThat(WeekCalendar.weekStartOf(LocalDate.of(2026, 10, 26))).isEqualTo(LocalDate.of(2026, 10, 26));
        assertThat(WeekCalendar.weekStartOf(LocalDate.of(2026, 11, 1))).isEqualTo(LocalDate.of(2026, 10, 26));
    }

    @Test
    void isMonday() {
        assertThat(WeekCalendar.isMonday(LocalDate.of(2026, 10, 19))).isTrue();
        assertThat(WeekCalendar.isMonday(LocalDate.of(2026, 10, 20))).isFalse();
        assertThat(WeekCalendar.isMonday(null)).isFalse();
    }

    @Test
    void pastWeekEndsAfterSundayInVietnam() {
        LocalDate week = LocalDate.of(2026, 10, 26);
        // Sunday 23:59 Vietnam time: still this week.
        assertThat(WeekCalendar.isPastWeek(week, at("2026-11-01T16:59:00Z"))).isFalse();
        // Monday 00:00 Vietnam time: the week is over.
        assertThat(WeekCalendar.isPastWeek(week, at("2026-11-01T17:00:00Z"))).isTrue();
        assertThat(WeekCalendar.isPastWeek(LocalDate.of(2026, 11, 2), at("2026-10-30T02:00:00Z"))).isFalse();
    }

    @Test
    void dateOfASlotAcrossTheYearEnd() {
        LocalDate week = LocalDate.of(2026, 12, 28);
        assertThat(WeekCalendar.dateOf(week, DayOfWeek.MONDAY)).isEqualTo(LocalDate.of(2026, 12, 28));
        assertThat(WeekCalendar.dateOf(week, DayOfWeek.SUNDAY)).isEqualTo(LocalDate.of(2027, 1, 3));
        assertThat(WeekCalendar.datesOf(week)).hasSize(7).startsWith(week).endsWith(LocalDate.of(2027, 1, 3));
    }

    @Test
    void dateOfRejectsANonMonday() {
        assertThatThrownBy(() -> WeekCalendar.dateOf(LocalDate.of(2026, 10, 27), DayOfWeek.MONDAY))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
