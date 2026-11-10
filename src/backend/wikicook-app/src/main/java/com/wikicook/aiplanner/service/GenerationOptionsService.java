package com.wikicook.aiplanner.service;

import com.wikicook.ai.quota.AiFeature;
import com.wikicook.ai.quota.AiQuotaService;
import com.wikicook.ai.quota.UserAllowance;
import com.wikicook.aiplanner.config.AiPlannerProperties;
import com.wikicook.aiplanner.dto.GenerationConstraints;
import com.wikicook.aiplanner.dto.GenerationOptionsResponse;
import com.wikicook.mealplan.api.MealPlanWriteFacade;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.domain.SlotKey;
import com.wikicook.mealplan.port.DietaryProfilePort;
import com.wikicook.mealplan.port.PlannerProfile;
import com.wikicook.mealplan.service.WeekCalendar;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * Data for the constraints dialog (research R12) and the slots a generation may fill. Everything here is in
 * Vietnam time. The profile is only read: what the user changes in the dialog applies to one generation (FR-004).
 */
@Service
public class GenerationOptionsService {

    private final DietaryProfilePort profiles;
    private final MealPlanWriteFacade mealPlans;
    private final AiQuotaService quota;
    private final AiPlannerProperties properties;
    private final Clock clock;

    /** The 030 beans are {@code @Lazy}: until 030 (or the dev stubs) provide them the app still starts. */
    public GenerationOptionsService(@Lazy DietaryProfilePort profiles, @Lazy MealPlanWriteFacade mealPlans,
                                    AiQuotaService quota, AiPlannerProperties properties, Clock clock) {
        this.profiles = profiles;
        this.mealPlans = mealPlans;
        this.quota = quota;
        this.properties = properties;
        this.clock = clock;
    }

    public GenerationOptionsResponse options(long userId, LocalDate weekStart) {
        PlannerProfile profile = profile(userId);
        UserAllowance allowance = quota.userAllowance(userId);
        return new GenerationOptionsResponse(
                weekStart,
                selectableDays(weekStart),
                List.copyOf(availableSlots(userId, weekStart)),
                defaults(profile),
                profile.allergenIds().isEmpty() && profile.excludedIngredientIds().isEmpty()
                        && profile.dietType() == DietType.NONE,
                allowance.remaining(),
                allowance.nextAvailableAt(),
                quota.dailyCapAvailable(AiFeature.MEAL_PLANNER));
    }

    public PlannerProfile profile(long userId) {
        PlannerProfile profile = profiles.findByUserId(userId);
        return profile == null ? PlannerProfile.empty() : profile;
    }

    /** Nothing for a past week, from today for the current week, all seven days for a later week. */
    public List<DayOfWeek> selectableDays(LocalDate weekStart) {
        LocalDate today = WeekCalendar.today(clock);
        List<DayOfWeek> days = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            if (!WeekCalendar.dateOf(weekStart, day).isBefore(today)) {
                days.add(day);
            }
        }
        return days;
    }

    /** Recipes already planned in the user's week (030), to avoid offering them again. */
    public Set<Long> plannedRecipeIds(long userId, LocalDate weekStart) {
        return mealPlans.plannedRecipeIds(userId, weekStart);
    }

    /** Empty slots of selectable days, without the meals of today already past their cutoff; day → meal order. */
    public Set<SlotKey> availableSlots(long userId, LocalDate weekStart) {
        List<DayOfWeek> days = selectableDays(weekStart);
        if (days.isEmpty()) {
            return Set.of();
        }
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), WeekCalendar.VIETNAM);
        Set<SlotKey> slots = new TreeSet<>();
        for (SlotKey slot : mealPlans.emptySlots(userId, weekStart)) {
            boolean today = WeekCalendar.dateOf(weekStart, slot.day()).equals(now.toLocalDate());
            boolean pastCutoff = today && !now.toLocalTime().isBefore(properties.mealCutoffs().of(slot.meal()));
            if (days.contains(slot.day()) && !pastCutoff) {
                slots.add(slot);
            }
        }
        return slots;
    }

    static GenerationConstraints defaults(PlannerProfile profile) {
        return new GenerationConstraints(profile.allergenIds(), profile.excludedIngredientIds(), profile.dietType(),
                profile.householdSize(), profile.maxCookTimeMinutes(), profile.preferredCuisineIds());
    }
}
