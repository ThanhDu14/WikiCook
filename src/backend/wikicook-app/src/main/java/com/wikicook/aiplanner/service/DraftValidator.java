package com.wikicook.aiplanner.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wikicook.aiplanner.domain.Candidate;
import com.wikicook.aiplanner.repository.EligibleRecipeQuery;
import com.wikicook.common.diet.DietRestrictions;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.SlotKey;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Step 4 of generation (research R7): checks the LLM answer against the requested slots, the candidates and, a
 * second time, the database. Bad picks are dropped and their slots left for the user ("Chọn món"); an answer
 * that does not fill exactly the requested slots asks for one retry.
 */
@Component
public class DraftValidator {

    static final String INVALID_JSON = "JSON không hợp lệ";

    private final EligibleRecipeQuery eligibleQuery;
    private final ObjectMapper json = new ObjectMapper();

    public DraftValidator(EligibleRecipeQuery eligibleQuery) {
        this.eligibleQuery = eligibleQuery;
    }

    public sealed interface ValidationResult permits Retry, Valid {
    }

    /** @param reason sent back to the LLM in the correction message */
    public record Retry(String reason) implements ValidationResult {
    }

    /**
     * @param picks        accepted recipe per slot, in day → meal order
     * @param chooseSlots  requested slots left empty, in day → meal order
     * @param removedPicks picks dropped by the checks (FR-032)
     */
    public record Valid(Map<SlotKey, Long> picks, List<SlotKey> chooseSlots, int removedPicks)
            implements ValidationResult {
    }

    public ValidationResult validate(String llmJson, List<SlotKey> requestedSlots,
                                     Map<Meal, List<Candidate>> candidates, DietRestrictions restrictions) {
        Map<SlotKey, Long> answer = new TreeMap<>();
        Set<SlotKey> repeated = new TreeSet<>();
        try {
            if (!parse(llmJson, answer, repeated)) {
                return new Retry(INVALID_JSON);
            }
        } catch (JsonProcessingException | IllegalArgumentException e) {
            return new Retry(INVALID_JSON);
        }
        Set<SlotKey> requested = new TreeSet<>(requestedSlots);
        if (!repeated.isEmpty()) {
            return new Retry("Trùng ô: " + describe(repeated));
        }
        Set<SlotKey> missing = new TreeSet<>(requested);
        missing.removeAll(answer.keySet());
        if (!missing.isEmpty()) {
            return new Retry("Thiếu ô: " + describe(missing));
        }
        Set<SlotKey> extra = new TreeSet<>(answer.keySet());
        extra.removeAll(requested);
        if (!extra.isEmpty()) {
            return new Retry("Thừa ô: " + describe(extra));
        }
        return check(answer, candidates, restrictions);
    }

    /** @return false when the shape is wrong; {@code repeated} collects slots answered more than once */
    private boolean parse(String llmJson, Map<SlotKey, Long> answer, Set<SlotKey> repeated)
            throws JsonProcessingException {
        if (llmJson == null) {
            return false;
        }
        JsonNode slots = json.readTree(llmJson).path("slots");
        if (!slots.isArray()) {
            return false;
        }
        for (JsonNode slot : slots) {
            JsonNode day = slot.path("day");
            JsonNode meal = slot.path("meal");
            JsonNode recipeId = slot.path("recipeId");
            if (!day.isTextual() || !meal.isTextual() || !recipeId.canConvertToLong() || !recipeId.isIntegralNumber()) {
                return false;
            }
            SlotKey key = new SlotKey(DayOfWeek.valueOf(day.asText()), Meal.valueOf(meal.asText()));
            if (answer.put(key, recipeId.asLong()) != null) {
                repeated.add(key);
            }
        }
        return true;
    }

    private Valid check(Map<SlotKey, Long> answer, Map<Meal, List<Candidate>> candidates,
                        DietRestrictions restrictions) {
        Map<Meal, Set<Long>> allowed = candidates.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
                e -> e.getValue().stream().map(Candidate::recipeId).collect(Collectors.toSet())));
        Set<Long> stillEligible = eligibleQuery.stillEligible(new TreeSet<>(answer.values()), restrictions);
        Map<SlotKey, Long> picks = new TreeMap<>();
        List<SlotKey> chooseSlots = new ArrayList<>();
        Set<Long> used = new HashSet<>();
        int removed = 0;
        // TreeMap order is day → meal, so the first use of a recipe is the one kept (FR-011).
        for (Map.Entry<SlotKey, Long> entry : answer.entrySet()) {
            long recipeId = entry.getValue();
            boolean ok = allowed.getOrDefault(entry.getKey().meal(), Set.of()).contains(recipeId)
                    && stillEligible.contains(recipeId)
                    && used.add(recipeId);
            if (ok) {
                picks.put(entry.getKey(), recipeId);
            } else {
                chooseSlots.add(entry.getKey());
                removed++;
            }
        }
        return new Valid(picks, chooseSlots, removed);
    }

    private static String describe(Set<SlotKey> slots) {
        return slots.stream().map(slot -> slot.day() + " " + slot.meal()).collect(Collectors.joining(", "));
    }
}
