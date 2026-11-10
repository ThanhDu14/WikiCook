package com.wikicook.aiplanner.service;

import com.wikicook.ai.llm.LlmRequest;
import com.wikicook.aiplanner.domain.Candidate;
import com.wikicook.mealplan.domain.Meal;
import com.wikicook.mealplan.domain.SlotKey;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Step 3 of generation (research R6): a Vietnamese system prompt from a versioned template, and a compact user
 * prompt of slots and candidates. Recipe titles are user content, so they are cut and flattened; whatever the
 * model answers still goes through {@code DraftValidator}. The builder receives no user data at all (NFR-003).
 */
@Component
public class PromptBuilder {

    public static final String TEMPLATE_VERSION = "meal-plan.v1";
    static final int TITLE_LENGTH = 80;
    static final double TEMPERATURE = 0.4;
    /** Gemini {@code responseSchema}: ids only, easy to check and cheap in tokens. */
    static final String RESPONSE_SCHEMA = """
            {"type":"OBJECT","properties":{"slots":{"type":"ARRAY","items":{"type":"OBJECT","properties":{\
            "day":{"type":"STRING","enum":["MONDAY","TUESDAY","WEDNESDAY","THURSDAY","FRIDAY","SATURDAY","SUNDAY"]},\
            "meal":{"type":"STRING","enum":["BREAKFAST","LUNCH","DINNER"]},\
            "recipeId":{"type":"INTEGER"}},"required":["day","meal","recipeId"]}}},"required":["slots"]}""";

    private final String systemPrompt = load("prompts/" + TEMPLATE_VERSION + ".txt");

    /**
     * @param candidates           per meal, from {@code CandidateService}
     * @param preferredCuisineNames shown as a preference only; may be empty
     */
    public LlmRequest build(List<SlotKey> slots, Map<Meal, List<Candidate>> candidates,
                            List<String> preferredCuisineNames) {
        return build(slots, candidates, preferredCuisineNames, null);
    }

    /**
     * @param seed when given, the candidates are listed in an order shuffled by it (one per generation), so the LLM
     *             does not keep picking the first lines; {@code null} lists them as ranked
     */
    public LlmRequest build(List<SlotKey> slots, Map<Meal, List<Candidate>> candidates,
                            List<String> preferredCuisineNames, Long seed) {
        StringBuilder prompt = new StringBuilder("Các ô cần điền (ngày | bữa):\n");
        slots.stream().sorted().forEach(slot -> prompt.append(slot.day()).append(" | ").append(slot.meal()).append('\n'));
        prompt.append("\nMón được phép chọn (id | tên | bữa | phút | ẩm thực | nguyên liệu chính):\n");
        List<Map.Entry<Candidate, Set<Meal>>> allowed = new ArrayList<>(allowedMeals(candidates).entrySet());
        if (seed != null) {
            Collections.shuffle(allowed, new Random(seed)); // the LLM leans on the first lines
        }
        allowed.forEach(entry -> prompt.append(line(entry.getKey(), entry.getValue())).append('\n'));
        if (!preferredCuisineNames.isEmpty()) {
            prompt.append("\nẨm thực ưa thích: ").append(String.join(", ", preferredCuisineNames)).append('\n');
        }
        return new LlmRequest(systemPrompt, prompt.toString().stripTrailing(), RESPONSE_SCHEMA, TEMPERATURE);
    }

    /** The retry of research R7: same request plus what was wrong with the first answer. */
    public LlmRequest correction(LlmRequest previous, String problem) {
        String prompt = previous.userPrompt() + "\n\nCâu trả lời trước không dùng được: " + problem
                + "\nHãy trả lại JSON điền đủ đúng các ô trên, chỉ dùng món trong danh sách.";
        return new LlmRequest(previous.systemPrompt(), prompt, previous.responseSchema(), previous.temperature());
    }

    /** Each candidate once, with the meals whose list it is in (first seen order). */
    private static Map<Candidate, Set<Meal>> allowedMeals(Map<Meal, List<Candidate>> candidates) {
        Map<Long, Candidate> byId = new LinkedHashMap<>();
        Map<Long, Set<Meal>> meals = new LinkedHashMap<>();
        for (Meal meal : Meal.values()) {
            for (Candidate candidate : candidates.getOrDefault(meal, List.of())) {
                byId.putIfAbsent(candidate.recipeId(), candidate);
                meals.computeIfAbsent(candidate.recipeId(), id -> EnumSet.noneOf(Meal.class)).add(meal);
            }
        }
        Map<Candidate, Set<Meal>> result = new LinkedHashMap<>();
        byId.forEach((id, candidate) -> result.put(candidate, meals.get(id)));
        return result;
    }

    private static String line(Candidate candidate, Set<Meal> meals) {
        List<String> cells = new ArrayList<>();
        cells.add(String.valueOf(candidate.recipeId()));
        cells.add(clean(candidate.title(), TITLE_LENGTH));
        cells.add(meals.stream().map(Meal::name).collect(Collectors.joining(",")));
        cells.add(candidate.totalTimeMinutes() == null ? "?" : String.valueOf(candidate.totalTimeMinutes()));
        cells.add(candidate.cuisineName() == null ? "" : clean(candidate.cuisineName(), 40));
        cells.add(candidate.mainIngredientNames().stream().map(name -> clean(name, 40))
                .collect(Collectors.joining(", ")));
        return String.join(" | ", cells);
    }

    /** One line, no column separator, at most {@code max} characters. */
    static String clean(String text, int max) {
        String flat = text.replace('|', '/').replaceAll("\\s+", " ").strip();
        return flat.length() <= max ? flat : flat.substring(0, max).strip();
    }

    private static String load(String path) {
        try (InputStream in = PromptBuilder.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing prompt template " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
