package com.wikicook.mealplan.service;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Weeks of the meal planner (030 T019): they start on Monday and follow the Vietnam calendar, whatever zone the
 * application {@link Clock} is in.
 */
public final class WeekCalendar {

    public static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

    private WeekCalendar() {
    }

    public static LocalDate today(Clock clock) {
        return LocalDate.ofInstant(clock.instant(), VIETNAM);
    }

    public static LocalDate currentWeekStart(Clock clock) {
        return weekStartOf(today(clock));
    }

    public static LocalDate weekStartOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    public static boolean isMonday(LocalDate date) {
        return date != null && date.getDayOfWeek() == DayOfWeek.MONDAY;
    }

    /** Whether the week's Sunday is already behind us in Vietnam. */
    public static boolean isPastWeek(LocalDate weekStart, Clock clock) {
        return weekStart.plusDays(6).isBefore(today(clock));
    }

    /** The calendar date of {@code day} in the week starting {@code weekStart}. */
    public static LocalDate dateOf(LocalDate weekStart, DayOfWeek day) {
        if (!isMonday(weekStart)) {
            throw new IllegalArgumentException("weekStart must be a Monday");
        }
        return weekStart.plusDays(day.getValue() - 1L);
    }

    public static List<LocalDate> datesOf(LocalDate weekStart) {
        return IntStream.rangeClosed(1, 7).mapToObj(DayOfWeek::of).map(day -> dateOf(weekStart, day)).toList();
    }
}
