package com.wikicook.aiplanner.domain;

import com.wikicook.mealplan.domain.Meal;
import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.Objects;

/** One cell of the week (data-model 3); ordered day → meal, the order drafts are filled and deduplicated in. */
public record SlotKey(DayOfWeek day, Meal meal) implements Comparable<SlotKey> {

    private static final Comparator<SlotKey> ORDER = Comparator.comparing(SlotKey::day).thenComparing(SlotKey::meal);

    public SlotKey {
        Objects.requireNonNull(day, "day");
        Objects.requireNonNull(meal, "meal");
    }

    @Override
    public int compareTo(SlotKey other) {
        return ORDER.compare(this, other);
    }
}
