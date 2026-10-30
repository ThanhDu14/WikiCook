package com.wikicook.aiplanner.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.wikicook.ai.config.AiMode;
import com.wikicook.ai.config.AiProperties;
import com.wikicook.ai.llm.LlmClient;
import com.wikicook.ai.quota.AiQuotaService;
import com.wikicook.ai.quota.Reservation;
import com.wikicook.ai.quota.UserAllowance;
import com.wikicook.ai.testing.MutableClock;
import com.wikicook.aiplanner.config.AiPlannerProperties;
import com.wikicook.aiplanner.domain.Candidate;
import com.wikicook.aiplanner.domain.RecipeCard;
import com.wikicook.aiplanner.dto.GenerateRequest;
import com.wikicook.aiplanner.dto.GenerationConstraints;
import com.wikicook.aiplanner.repository.EligibleRecipeQuery;
import com.wikicook.aiplanner.repository.RecipeCardQuery;
import com.wikicook.mealplan.api.MealPlanWriteFacade;
import com.wikicook.mealplan.domain.DietType;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.SlotKey;
import com.wikicook.mealplan.port.DietaryProfilePort;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Shared set-up of the GenerationService unit tests: next week (2 November 2026) seen on Wednesday 28 October at
 * 09:00 Vietnam time; the user asks for Monday and Tuesday, all meals, and has already planned Monday lunch, so
 * five slots are to be filled. Each meal has five candidates: breakfast 1–5, lunch 11–15, dinner 21–25.
 */
class GenerationFixture {

    static final long USER = 7;
    static final long ROW = 100;
    static final LocalDate WEEK = LocalDate.of(2026, 11, 2);
    static final Instant NOW = OffsetDateTime.of(2026, 10, 28, 9, 0, 0, 0, ZoneOffset.ofHours(7)).toInstant();
    static final Duration BUDGET = Duration.ofSeconds(60);
    static final SlotKey MON_BREAKFAST = new SlotKey(DayOfWeek.MONDAY, Meal.BREAKFAST);
    static final SlotKey MON_LUNCH = new SlotKey(DayOfWeek.MONDAY, Meal.LUNCH);
    static final SlotKey MON_DINNER = new SlotKey(DayOfWeek.MONDAY, Meal.DINNER);
    static final SlotKey TUE_BREAKFAST = new SlotKey(DayOfWeek.TUESDAY, Meal.BREAKFAST);
    static final SlotKey TUE_LUNCH = new SlotKey(DayOfWeek.TUESDAY, Meal.LUNCH);
    static final SlotKey TUE_DINNER = new SlotKey(DayOfWeek.TUESDAY, Meal.DINNER);
    static final List<SlotKey> REQUESTED = List.of(MON_BREAKFAST, MON_DINNER, TUE_BREAKFAST, TUE_LUNCH, TUE_DINNER);

    final MutableClock clock = new MutableClock(NOW);
    final DietaryProfilePort profiles = mock(DietaryProfilePort.class);
    final MealPlanWriteFacade facade = mock(MealPlanWriteFacade.class);
    final AiQuotaService quota = mock(AiQuotaService.class);
    final EligibleRecipeQuery eligible = mock(EligibleRecipeQuery.class);
    final CandidateService candidateService = mock(CandidateService.class);
    final RecipeCardQuery cards = mock(RecipeCardQuery.class);
    final InFlightGenerations inFlight = new InFlightGenerations();
    final Map<Meal, List<Candidate>> candidates = new EnumMap<>(Meal.class);

    GenerationFixture() {
        candidates.put(Meal.BREAKFAST, candidates(Meal.BREAKFAST, 1));
        candidates.put(Meal.LUNCH, candidates(Meal.LUNCH, 11));
        candidates.put(Meal.DINNER, candidates(Meal.DINNER, 21));

        Set<SlotKey> empty = new TreeSet<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            for (Meal meal : Meal.values()) {
                empty.add(new SlotKey(day, meal));
            }
        }
        empty.remove(MON_LUNCH);
        when(facade.emptySlots(USER, WEEK)).thenReturn(empty);
        when(quota.userAllowance(anyLong())).thenReturn(new UserAllowance(5, null));
        when(quota.reserve(any(), any(), any(), any(), any(), any(), any())).thenReturn(new Reservation(ROW, true));
        when(eligible.eligibleByMeal(anySet(), any(), any())).thenAnswer(call -> {
            Map<Meal, List<Long>> result = new EnumMap<>(Meal.class);
            for (Meal meal : call.<Set<Meal>>getArgument(0)) {
                result.put(meal, candidates.get(meal).stream().map(Candidate::recipeId).toList());
            }
            return result;
        });
        when(eligible.stillEligible(anyCollection(), any()))
                .thenAnswer(call -> new HashSet<>(call.<Collection<Long>>getArgument(0)));
        when(candidateService.candidates(anyMap(), anyMap(), any())).thenReturn(candidates);
        when(candidateService.cuisineNames(anySet())).thenReturn(List.of());
        when(cards.cards(anyCollection())).thenAnswer(call -> {
            Map<Long, RecipeCard> result = new LinkedHashMap<>();
            for (Long id : call.<Collection<Long>>getArgument(0)) {
                result.put(id, new RecipeCard(id, "Món " + id, null, 20, DietType.NONE));
            }
            return result;
        });
    }

    private static List<Candidate> candidates(Meal meal, long firstId) {
        List<Candidate> list = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            list.add(new Candidate(firstId + i, "Món " + (firstId + i), Set.of(meal), 20, null, null, null,
                    List.of(), List.of(), i));
        }
        return list;
    }

    GenerationService service(LlmClient llm) {
        AiPlannerProperties plannerProperties = new AiPlannerProperties(new AiPlannerProperties.MealCutoffs(
                LocalTime.of(10, 0), LocalTime.of(14, 0), LocalTime.of(20, 0)));
        GenerationOptionsService options = new GenerationOptionsService(profiles, facade, quota, plannerProperties,
                clock);
        AiProperties aiProperties = new AiProperties(AiMode.MOCK, null, "http://localhost",
                new AiProperties.Llm("fake", BUDGET),
                new AiProperties.Embedding("fake-embedding", 768, Duration.ofSeconds(30), false, 50,
                        Duration.ZERO, 0),
                new AiProperties.DailyCap(100, 0.7, 0.3));
        return new GenerationService(options, eligible, candidateService, new PromptBuilder(), llm,
                new DraftValidator(eligible), new BackupPlanner(), cards, quota, inFlight, aiProperties, clock);
    }

    static GenerationConstraints constraints(Integer servings) {
        return new GenerationConstraints(Set.of(9L), Set.of(), DietType.NONE, servings, null, Set.of());
    }

    static GenerateRequest request(GenerationConstraints constraints) {
        return new GenerateRequest(UUID.randomUUID(), EnumSet.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
                EnumSet.allOf(Meal.class), constraints);
    }

    static GenerateRequest request() {
        return request(constraints(4));
    }

    /** LLM reply for the given slot → recipe pairs. */
    static String reply(Map<SlotKey, Long> picks) {
        return picks.entrySet().stream()
                .map(e -> "{\"day\":\"%s\",\"meal\":\"%s\",\"recipeId\":%d}"
                        .formatted(e.getKey().day(), e.getKey().meal(), e.getValue()))
                .collect(Collectors.joining(",", "{\"slots\":[", "]}"));
    }

    /** A valid reply for the five requested slots. */
    static String validReply() {
        Map<SlotKey, Long> picks = new LinkedHashMap<>();
        picks.put(MON_BREAKFAST, 1L);
        picks.put(MON_DINNER, 21L);
        picks.put(TUE_BREAKFAST, 2L);
        picks.put(TUE_LUNCH, 11L);
        picks.put(TUE_DINNER, 22L);
        return reply(picks);
    }
}
