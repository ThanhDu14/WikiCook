package com.wikicook.aiplanner.service;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.wikicook.ai.quota.AiFeature;
import com.wikicook.ai.quota.AiQuotaService;
import com.wikicook.ai.quota.UserAllowance;
import com.wikicook.aiplanner.config.AiPlannerProperties;
import com.wikicook.aiplanner.dto.GenerationConstraints;
import com.wikicook.aiplanner.dto.GenerationOptionsResponse;
import com.wikicook.mealplan.api.MealPlanWriteFacade;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.SlotKey;
import com.wikicook.mealplan.port.DietaryProfilePort;
import com.wikicook.mealplan.port.PlannerProfile;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** T028: data for the constraints dialog (research R12). */
class GenerationOptionsServiceTest {

    private static final long USER = 7;
    /** Week of Monday 26 October 2026; "today" in most tests is Wednesday 28 October. */
    private static final LocalDate THIS_WEEK = LocalDate.of(2026, 10, 26);
    private static final LocalDate NEXT_WEEK = THIS_WEEK.plusWeeks(1);
    private static final AiPlannerProperties PROPERTIES = new AiPlannerProperties(new AiPlannerProperties.MealCutoffs(
            LocalTime.of(10, 0), LocalTime.of(14, 0), LocalTime.of(20, 0)));

    private final DietaryProfilePort profiles = mock(DietaryProfilePort.class);
    private final MealPlanWriteFacade facade = mock(MealPlanWriteFacade.class);
    private final AiQuotaService quota = mock(AiQuotaService.class);

    private static Clock vietnamTime(int year, int month, int day, int hour, int minute) {
        Instant instant = OffsetDateTime.of(year, month, day, hour, minute, 0, 0, ZoneOffset.ofHours(7)).toInstant();
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    private static Set<SlotKey> allSlots() {
        Set<SlotKey> slots = new TreeSet<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            for (Meal meal : Meal.values()) {
                slots.add(new SlotKey(day, meal));
            }
        }
        return slots;
    }

    private GenerationOptionsService service(Clock clock) {
        return new GenerationOptionsService(profiles, facade, quota, PROPERTIES, clock);
    }

    private GenerationOptionsService wednesdayMorning() {
        return service(vietnamTime(2026, 10, 28, 9, 0));
    }

    @BeforeEach
    void setUp() {
        when(profiles.findByUserId(USER)).thenReturn(PlannerProfile.empty());
        when(facade.emptySlots(eq(USER), org.mockito.ArgumentMatchers.any())).thenReturn(allSlots());
        when(quota.userAllowance(anyLong())).thenReturn(new UserAllowance(5, null));
        when(quota.dailyCapAvailable(AiFeature.MEAL_PLANNER)).thenReturn(true);
    }

    @Test
    void defaultsComeFromTheProfile_US1_1() {
        when(profiles.findByUserId(USER)).thenReturn(
                new PlannerProfile(4, DietType.PESCATARIAN, 45, Set.of(1L), Set.of(10L, 11L), Set.of(3L)));

        GenerationOptionsResponse options = wednesdayMorning().options(USER, NEXT_WEEK);

        assertThat(options.defaults()).isEqualTo(
                new GenerationConstraints(Set.of(1L), Set.of(10L, 11L), DietType.PESCATARIAN, 4, 45, Set.of(3L)));
        assertThat(options.profileIncomplete()).isFalse();
        assertThat(options.weekStart()).isEqualTo(NEXT_WEEK);
    }

    @Test
    void noHouseholdSizeMeansServingsAsInTheRecipe() {
        when(profiles.findByUserId(USER)).thenReturn(
                new PlannerProfile(null, DietType.VEGAN, null, Set.of(), Set.of(), Set.of()));

        GenerationConstraints defaults = wednesdayMorning().options(USER, NEXT_WEEK).defaults();

        assertThat(defaults.servings()).isNull();
        assertThat(defaults.maxCookTimeMinutes()).isNull();
    }

    @Test
    void emptyOrMissingProfileIsIncomplete_US1_8() {
        assertThat(wednesdayMorning().options(USER, NEXT_WEEK).profileIncomplete()).isTrue();

        when(profiles.findByUserId(USER)).thenReturn(null);
        GenerationOptionsResponse options = wednesdayMorning().options(USER, NEXT_WEEK);
        assertThat(options.profileIncomplete()).isTrue();
        assertThat(options.defaults()).isEqualTo(
                new GenerationConstraints(Set.of(), Set.of(), DietType.NONE, null, null, Set.of()));
    }

    @Test
    void anyHardRuleMakesTheProfileComplete() {
        when(profiles.findByUserId(USER)).thenReturn(
                new PlannerProfile(2, DietType.NONE, null, Set.of(), Set.of(5L), Set.of()));
        assertThat(wednesdayMorning().options(USER, NEXT_WEEK).profileIncomplete()).isFalse();

        when(profiles.findByUserId(USER)).thenReturn(
                new PlannerProfile(2, DietType.VEGETARIAN, null, Set.of(), Set.of(), Set.of()));
        assertThat(wednesdayMorning().options(USER, NEXT_WEEK).profileIncomplete()).isFalse();
    }

    @Test
    void nextWeekOffersEveryDayAndEverySlot_US1_2() {
        GenerationOptionsResponse options = wednesdayMorning().options(USER, NEXT_WEEK);

        assertThat(options.selectableDays()).containsExactly(DayOfWeek.values());
        assertThat(options.emptySlots()).hasSize(21);
        verify(facade).emptySlots(USER, NEXT_WEEK);
    }

    @Test
    void currentWeekStartsToday_US1_2() {
        GenerationOptionsResponse options = wednesdayMorning().options(USER, THIS_WEEK);

        assertThat(options.selectableDays()).containsExactly(WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY);
        assertThat(options.emptySlots()).hasSize(15).first().isEqualTo(new SlotKey(WEDNESDAY, Meal.BREAKFAST));
        assertThat(options.emptySlots()).noneMatch(slot -> slot.day() == MONDAY || slot.day() == TUESDAY);
    }

    @Test
    void mealsOfTodayPastTheirCutoffAreDropped() {
        GenerationOptionsResponse at15 = service(vietnamTime(2026, 10, 28, 15, 0)).options(USER, THIS_WEEK);
        assertThat(at15.emptySlots()).first().isEqualTo(new SlotKey(WEDNESDAY, Meal.DINNER));
        assertThat(at15.emptySlots()).hasSize(13);

        // Exactly at the cutoff the meal is gone.
        GenerationOptionsResponse at10 = service(vietnamTime(2026, 10, 28, 10, 0)).options(USER, THIS_WEEK);
        assertThat(at10.emptySlots()).first().isEqualTo(new SlotKey(WEDNESDAY, Meal.LUNCH));
    }

    @Test
    void sundayNightHasNoSlotLeft() {
        GenerationOptionsResponse options = service(vietnamTime(2026, 11, 1, 21, 0)).options(USER, THIS_WEEK);

        assertThat(options.selectableDays()).containsExactly(SUNDAY);
        assertThat(options.emptySlots()).isEmpty();
    }

    @Test
    void pastWeekHasNothingToSelect() {
        GenerationOptionsResponse options = wednesdayMorning().options(USER, THIS_WEEK.minusWeeks(1));

        assertThat(options.selectableDays()).isEmpty();
        assertThat(options.emptySlots()).isEmpty();
    }

    @Test
    void plannedSlotsAreNotOffered_FR_007() {
        Set<SlotKey> empty = allSlots();
        empty.remove(new SlotKey(MONDAY, Meal.LUNCH));
        when(facade.emptySlots(USER, NEXT_WEEK)).thenReturn(empty);

        assertThat(wednesdayMorning().options(USER, NEXT_WEEK).emptySlots())
                .hasSize(20).doesNotContain(new SlotKey(MONDAY, Meal.LUNCH));
    }

    @Test
    void remainingAndDailyCapComeFromTheQuota() {
        Instant next = Instant.parse("2026-10-28T03:00:00Z");
        when(quota.userAllowance(USER)).thenReturn(new UserAllowance(0, next));
        when(quota.dailyCapAvailable(AiFeature.MEAL_PLANNER)).thenReturn(false);

        GenerationOptionsResponse options = wednesdayMorning().options(USER, NEXT_WEEK);

        assertThat(options.remaining()).isZero();
        assertThat(options.nextAvailableAt()).isEqualTo(next);
        assertThat(options.aiAvailableToday()).isFalse();
    }

    @Test
    void profileIsOnlyRead_FR_004() {
        wednesdayMorning().options(USER, NEXT_WEEK);

        verify(profiles).findByUserId(USER);
        verifyNoMoreInteractions(profiles);
    }

    @Test
    void availableSlotsIsWhatGenerationMayFill() {
        assertThat(wednesdayMorning().availableSlots(USER, THIS_WEEK)).hasSize(15);
        assertThat(service(vietnamTime(2026, 11, 1, 21, 0)).availableSlots(USER, THIS_WEEK)).isEmpty();
        assertThat(EnumSet.copyOf(wednesdayMorning().selectableDays(NEXT_WEEK))).hasSize(7);
    }
}
