package com.wikicook.ai.quota;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.ai.config.AiProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Limits counted on {@code ai_requests} (research R11): a daily cap per feature shared by 031 and 090, and
 * {@value #HOURLY_LIMIT} counted meal planner generations per user in a rolling hour.
 */
public class AiQuotaService {

    public static final int HOURLY_LIMIT = 5;
    static final Duration WINDOW = Duration.ofMinutes(60);
    static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int REASON_LENGTH = 100;

    private final AiRequestRepository repository;
    private final TransactionTemplate transaction;
    private final AiProperties.DailyCap dailyCap;
    private final Clock clock;
    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    public AiQuotaService(AiRequestRepository repository, TransactionTemplate transaction,
                          AiProperties.DailyCap dailyCap, Clock clock) {
        this.repository = repository;
        this.transaction = transaction;
        this.dailyCap = dailyCap;
        this.clock = clock;
    }

    /**
     * Writes the {@code ai_requests} row (outcome {@code FAILED} until {@link #finish}) and decides whether the
     * feature still has room in today's cap. The advisory lock makes check and insert atomic across instances.
     */
    public Reservation reserve(AiFeature feature, AiTrigger trigger, Long userId, Long recipeId,
                               LocalDate weekStart, Integer requestedSlots, Object constraints) {
        String constraintsJson = toJson(constraints);
        return transaction.execute(status -> {
            repository.lockFeature(feature);
            Instant now = clock.instant();
            boolean allowed = repository.countCalledLlmSince(feature, startOfDay(now)) < cap(feature);
            long id = repository.insert(new AiRequest(null, feature, userId, recipeId, null, trigger,
                    AiOutcome.FAILED, allowed, false, weekStart, requestedSlots, null, constraintsJson, null, now));
            return new Reservation(id, allowed);
        });
    }

    /** @param calledLlm whether the provider was really called; a reserved row that skipped it frees its place */
    public void finish(long requestRowId, AiOutcome outcome, boolean calledLlm, Integer removedPicks, String reason) {
        String trimmed = reason == null || reason.length() <= REASON_LENGTH ? reason : reason.substring(0, REASON_LENGTH);
        repository.finish(requestRowId, outcome, calledLlm, removedPicks, trimmed);
    }

    public UserAllowance userAllowance(long userId) {
        AiRequestRepository.CountedWindow window = repository.countedAfter(userId, clock.instant().minus(WINDOW));
        int remaining = Math.max(0, HOURLY_LIMIT - window.count());
        return new UserAllowance(remaining, remaining > 0 ? null : window.oldest().plus(WINDOW));
    }

    private int cap(AiFeature feature) {
        return switch (feature) {
            case MEAL_PLANNER -> dailyCap.mealPlanner();
            case NUTRITION -> dailyCap.nutrition();
        };
    }

    private static Instant startOfDay(Instant now) {
        return LocalDate.ofInstant(now, VIETNAM).atStartOfDay(VIETNAM).toInstant();
    }

    private String toJson(Object constraints) {
        if (constraints == null) {
            return null;
        }
        try {
            return json.writeValueAsString(constraints);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("constraints cannot be written as JSON", e);
        }
    }
}
