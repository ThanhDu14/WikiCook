package com.wikicook.aiplanner.service;

import com.wikicook.ai.AiUnavailableException;
import com.wikicook.ai.config.AiProperties;
import com.wikicook.ai.llm.CancellationToken;
import com.wikicook.ai.llm.LlmClient;
import com.wikicook.ai.llm.LlmRequest;
import com.wikicook.ai.quota.AiFeature;
import com.wikicook.ai.quota.AiOutcome;
import com.wikicook.ai.quota.AiQuotaService;
import com.wikicook.ai.quota.AiTrigger;
import com.wikicook.ai.quota.Reservation;
import com.wikicook.ai.quota.UserAllowance;
import com.wikicook.aiplanner.controller.BackupUnavailableException;
import com.wikicook.aiplanner.controller.NoEmptySlotException;
import com.wikicook.aiplanner.controller.NotEnoughRecipesException;
import com.wikicook.aiplanner.controller.PastWeekException;
import com.wikicook.aiplanner.controller.RateLimitedException;
import com.wikicook.aiplanner.domain.Candidate;
import com.wikicook.aiplanner.domain.RecipeCard;
import com.wikicook.aiplanner.domain.RelaxSuggestion;
import com.wikicook.aiplanner.dto.DraftItemDto;
import com.wikicook.aiplanner.dto.DraftMode;
import com.wikicook.aiplanner.dto.DraftNotice;
import com.wikicook.aiplanner.dto.DraftResponse;
import com.wikicook.aiplanner.dto.GenerateRequest;
import com.wikicook.aiplanner.dto.GenerationConstraints;
import com.wikicook.aiplanner.repository.EligibleRecipeQuery;
import com.wikicook.aiplanner.repository.RecipeCardQuery;
import com.wikicook.aiplanner.service.DraftValidator.Retry;
import com.wikicook.aiplanner.service.DraftValidator.Valid;
import com.wikicook.aiplanner.service.DraftValidator.ValidationResult;
import com.wikicook.mealplan.domain.ItemSource;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.SlotKey;
import com.wikicook.mealplan.service.WeekCalendar;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * One generation (data-model 4): check the week and slots, reserve the {@code ai_requests} row, filter (step 1),
 * retrieve (step 2), ask the LLM and check its answer (steps 3–4, one retry), or fall back to the planner without
 * AI (step 5). Synchronous within the LLM budget (research R9). Logs counts and durations only, never the prompt
 * or the reply.
 */
@Service
public class GenerationService {

    private static final Logger log = LoggerFactory.getLogger(GenerationService.class);
    /** A retry starts only with at least this much of the budget left (research R9). */
    static final Duration MIN_RETRY_BUDGET = Duration.ofSeconds(15);

    private final GenerationOptionsService options;
    private final EligibleRecipeQuery eligibleQuery;
    private final CandidateService candidateService;
    private final PromptBuilder promptBuilder;
    private final LlmClient llm;
    private final DraftValidator validator;
    private final BackupPlanner backupPlanner;
    private final RecipeCardQuery cards;
    private final AiQuotaService quota;
    private final InFlightGenerations inFlight;
    private final Duration budget;
    private final Clock clock;

    public GenerationService(GenerationOptionsService options, EligibleRecipeQuery eligibleQuery,
                             CandidateService candidateService, PromptBuilder promptBuilder, LlmClient llm,
                             DraftValidator validator, BackupPlanner backupPlanner, RecipeCardQuery cards,
                             AiQuotaService quota, InFlightGenerations inFlight, AiProperties aiProperties,
                             Clock clock) {
        this.options = options;
        this.eligibleQuery = eligibleQuery;
        this.candidateService = candidateService;
        this.promptBuilder = promptBuilder;
        this.llm = llm;
        this.validator = validator;
        this.backupPlanner = backupPlanner;
        this.cards = cards;
        this.quota = quota;
        this.inFlight = inFlight;
        this.budget = aiProperties.llm().timeout();
        this.clock = clock;
    }

    /** Picks per slot and how they were made; {@code calledLlm} is set as soon as the provider is asked. */
    private static final class Attempt {
        Map<SlotKey, Long> picks = Map.of();
        List<SlotKey> chooseSlots = List.of();
        int removedPicks;
        DraftMode mode;
        DraftNotice notice;
        AiOutcome outcome;
        String reason;
        boolean calledLlm;
        int llmCalls;
    }

    public DraftResponse generate(long userId, LocalDate weekStart, GenerateRequest request) {
        Instant started = clock.instant();
        if (WeekCalendar.isPastWeek(weekStart, clock)) {
            throw new PastWeekException();
        }
        try (InFlightGenerations.Running running = inFlight.begin(userId, request.requestId())) {
            List<SlotKey> slots = options.availableSlots(userId, weekStart).stream()
                    .filter(slot -> request.days().contains(slot.day()) && request.meals().contains(slot.meal()))
                    .toList();
            if (slots.isEmpty()) {
                throw new NoEmptySlotException();
            }
            UserAllowance allowance = quota.userAllowance(userId);
            if (allowance.remaining() == 0) {
                throw new RateLimitedException(allowance.nextAvailableAt());
            }
            GenerationConstraints constraints = request.constraints();
            Reservation reservation = quota.reserve(AiFeature.MEAL_PLANNER, AiTrigger.USER, userId, null, weekStart,
                    slots.size(), constraints);
            Attempt attempt = new Attempt();
            try {
                run(slots, constraints, reservation.llmAllowed(), started.plus(budget), running.token(), attempt);
            } catch (NotEnoughRecipesException e) {
                quota.finish(reservation.requestRowId(), AiOutcome.NOT_ENOUGH, false, null, null);
                running.finished(reservation.requestRowId(), AiOutcome.NOT_ENOUGH);
                throw e;
            } catch (RuntimeException e) {
                quota.finish(reservation.requestRowId(), AiOutcome.FAILED, attempt.calledLlm, null, "BACKUP_FAILED");
                running.finished(reservation.requestRowId(), AiOutcome.FAILED);
                throw new BackupUnavailableException(e);
            }
            quota.finish(reservation.requestRowId(), attempt.outcome, attempt.calledLlm,
                    attempt.mode == DraftMode.AI ? attempt.removedPicks : null, attempt.reason);
            running.finished(reservation.requestRowId(), attempt.outcome);
            DraftResponse draft = response(request, weekStart, constraints, attempt, quota.userAllowance(userId));
            log.info("Generation of {} slots: mode={} outcome={} items={} chooseSlots={} removedPicks={} llmCalls={}"
                            + " in {} ms", slots.size(), attempt.mode, attempt.outcome, draft.items().size(),
                    draft.chooseSlots().size(), attempt.removedPicks, attempt.llmCalls,
                    Duration.between(started, clock.instant()).toMillis());
            return draft;
        }
    }

    private void run(List<SlotKey> slots, GenerationConstraints constraints, boolean llmAllowed, Instant deadline,
                     CancellationToken token, Attempt attempt) {
        Map<Meal, Integer> slotsPerMeal = new EnumMap<>(Meal.class);
        slots.forEach(slot -> slotsPerMeal.merge(slot.meal(), 1, Integer::sum));
        Map<Meal, List<Long>> eligible = eligibleQuery.eligibleByMeal(slotsPerMeal.keySet(),
                constraints.restrictions(), constraints.maxCookTimeMinutes());
        checkEnough(slotsPerMeal, eligible, constraints);
        Map<Meal, List<Candidate>> candidates = candidateService.candidates(eligible, slotsPerMeal, constraints);
        if (!llmAllowed) {
            backup(slots, candidates, constraints, DraftNotice.AI_DAILY_CAP, AiOutcome.QUOTA_CAP, null, attempt);
            return;
        }
        try {
            LlmRequest prompt = promptBuilder.build(slots, candidates,
                    candidateService.cuisineNames(constraints.preferredCuisineIds()));
            ValidationResult result = ask(prompt, slots, candidates, constraints, deadline, token, attempt);
            if (result instanceof Retry retry) {
                if (remaining(deadline).compareTo(MIN_RETRY_BUDGET) < 0) {
                    backup(slots, candidates, constraints, DraftNotice.AUTO_NO_AI, AiOutcome.AUTO_DRAFT,
                            "RETRY_NO_TIME", attempt);
                    return;
                }
                result = ask(promptBuilder.correction(prompt, retry.reason()), slots, candidates, constraints,
                        deadline, token, attempt);
            }
            if (result instanceof Valid valid) {
                attempt.picks = valid.picks();
                attempt.chooseSlots = valid.chooseSlots();
                attempt.removedPicks = valid.removedPicks();
                attempt.mode = DraftMode.AI;
                attempt.outcome = AiOutcome.AI_DRAFT;
                return;
            }
            backup(slots, candidates, constraints, DraftNotice.AUTO_NO_AI, AiOutcome.AUTO_DRAFT, "RETRY_FAILED",
                    attempt);
        } catch (AiUnavailableException e) {
            boolean timedOut = !clock.instant().isBefore(deadline);
            backup(slots, candidates, constraints, DraftNotice.AUTO_NO_AI,
                    timedOut ? AiOutcome.TIMEOUT : AiOutcome.AUTO_DRAFT, timedOut ? "TIMEOUT" : "LLM_UNAVAILABLE",
                    attempt);
        }
    }

    private ValidationResult ask(LlmRequest request, List<SlotKey> slots, Map<Meal, List<Candidate>> candidates,
                                 GenerationConstraints constraints, Instant deadline, CancellationToken token,
                                 Attempt attempt) {
        Duration left = remaining(deadline);
        if (left.isZero()) {
            throw new AiUnavailableException("Generation budget spent");
        }
        attempt.calledLlm = true;
        attempt.llmCalls++;
        long start = System.nanoTime();
        String reply = llm.complete(request, left, token).text();
        log.debug("LLM call {} answered in {} ms", attempt.llmCalls, (System.nanoTime() - start) / 1_000_000);
        return validator.validate(reply, slots, candidates, constraints.restrictions());
    }

    private void backup(List<SlotKey> slots, Map<Meal, List<Candidate>> candidates, GenerationConstraints constraints,
                        DraftNotice notice, AiOutcome outcome, String reason, Attempt attempt) {
        BackupPlanner.Plan plan = backupPlanner.plan(slots, candidates, constraints.preferredCuisineIds());
        attempt.picks = plan.picks();
        attempt.chooseSlots = plan.chooseSlots();
        attempt.removedPicks = 0;
        attempt.mode = DraftMode.AUTO;
        attempt.notice = notice;
        attempt.outcome = outcome;
        attempt.reason = reason;
    }

    /** US4: a meal with fewer eligible recipes than slots cannot be planned without repeats (FR-012, FR-024). */
    private static void checkEnough(Map<Meal, Integer> slotsPerMeal, Map<Meal, List<Long>> eligible,
                                    GenerationConstraints constraints) {
        for (Map.Entry<Meal, Integer> entry : slotsPerMeal.entrySet()) {
            int count = eligible.getOrDefault(entry.getKey(), List.of()).size();
            if (count < entry.getValue()) {
                List<RelaxSuggestion> suggestions = new ArrayList<>();
                if (constraints.maxCookTimeMinutes() != null) {
                    suggestions.add(RelaxSuggestion.INCREASE_MAX_COOK_TIME);
                }
                if (!constraints.preferredCuisineIds().isEmpty()) {
                    suggestions.add(RelaxSuggestion.REMOVE_PREFERRED_CUISINES);
                }
                suggestions.add(RelaxSuggestion.SELECT_FEWER_SLOTS);
                throw new NotEnoughRecipesException(count, entry.getValue(), suggestions);
            }
        }
    }

    private DraftResponse response(GenerateRequest request, LocalDate weekStart, GenerationConstraints constraints,
                                   Attempt attempt, UserAllowance allowance) {
        Map<Long, RecipeCard> byId = cards.cards(attempt.picks.values());
        ItemSource source = attempt.mode == DraftMode.AI ? ItemSource.AI : ItemSource.AUTO;
        List<DraftItemDto> items = new ArrayList<>();
        TreeSet<SlotKey> chooseSlots = new TreeSet<>(attempt.chooseSlots);
        attempt.picks.forEach((slot, recipeId) -> {
            RecipeCard card = byId.get(recipeId);
            if (card == null) {
                chooseSlots.add(slot); // deleted since step 1
                return;
            }
            items.add(new DraftItemDto(slot.day(), slot.meal(), recipeId, card.title(), card.coverImageUrl(),
                    card.totalTimeMinutes(), constraints.servings(), source, card.dietType()));
        });
        return new DraftResponse(request.requestId(), weekStart, attempt.mode, attempt.notice, List.copyOf(items),
                List.copyOf(chooseSlots), attempt.removedPicks, allowance.remaining(), allowance.nextAvailableAt());
    }

    private Duration remaining(Instant deadline) {
        Duration left = Duration.between(clock.instant(), deadline);
        return left.isNegative() ? Duration.ZERO : left;
    }
}
