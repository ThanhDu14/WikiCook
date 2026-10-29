package com.wikicook.ai.quota;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.wikicook.ai.db.AbstractPostgresIT;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

/** V302: the CHECKs and cascades that the quota and embedding code rely on. */
class AiSchemaIT extends AbstractPostgresIT {

    private long userId;
    private long recipeId;

    @BeforeEach
    void seed() {
        userId = insertUser("schema-" + UUID.randomUUID() + "@wikicook.test");
        recipeId = insertRecipe();
    }

    private void insertRequest(String feature, String trigger, String outcome, boolean counted,
                               Long user, Long recipe, boolean withWeek) {
        jdbc.update("""
                INSERT INTO ai_requests (feature, trigger, outcome, called_llm, counted, user_id, recipe_id, week_start)
                VALUES (?, ?, ?, true, ?, ?, ?, CASE WHEN ? THEN DATE '2026-11-02' END)
                """, feature, trigger, outcome, counted, user, recipe, withWeek);
    }

    @Test
    void acceptsValidRows() {
        insertRequest("MEAL_PLANNER", "USER", "AI_DRAFT", true, userId, null, true);
        insertRequest("MEAL_PLANNER", "USER", "AUTO_DRAFT", false, userId, null, true);
        insertRequest("NUTRITION", "APPROVAL", "SUCCESS", false, null, recipeId, false);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM ai_requests WHERE user_id = ? OR recipe_id = ?",
                Integer.class, userId, recipeId)).isEqualTo(3);
    }

    @ParameterizedTest(name = "{0}/{1}/{2} counted={3}")
    @CsvSource({
            "CHAT,         USER,     FAILED,   false", // feature outside the list
            "MEAL_PLANNER, CRON,     FAILED,   false", // trigger outside the list
            "MEAL_PLANNER, USER,     DONE,     false", // outcome outside the list
            "MEAL_PLANNER, USER,     AI_DRAFT, false", // AI_DRAFT of the planner must be counted
            "MEAL_PLANNER, USER,     FAILED,   true",  // only AI_DRAFT is counted
            "NUTRITION,    APPROVAL, SUCCESS,  true",  // nutrition is never counted
    })
    void rejectsValuesOutsideTheChecks(String feature, String trigger, String outcome, boolean counted) {
        assertThatThrownBy(() -> insertRequest(feature, trigger, outcome, counted, userId, recipeId, true))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void mealPlannerNeedsUserAndWeek() {
        assertThatThrownBy(() -> insertRequest("MEAL_PLANNER", "USER", "FAILED", false, null, null, true))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertRequest("MEAL_PLANNER", "USER", "FAILED", false, userId, null, false))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void nutritionNeedsRecipe() {
        assertThatThrownBy(() -> insertRequest("NUTRITION", "APPROVAL", "FAILED", false, null, null, false))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void embeddingMustHave768Dimensions() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO recipe_embeddings (recipe_id, embedding, model, content_hash) VALUES (?, ?::vector, 'm', ?)",
                recipeId, "[1,2,3]", "a".repeat(64)))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("768");
    }

    @Test
    void deletingRecipeRemovesItsEmbeddingAndNutritionLog() {
        jdbc.update("INSERT INTO recipe_embeddings (recipe_id, embedding, model, content_hash) VALUES (?, ?::vector, 'm', ?)",
                recipeId, vector768(), "a".repeat(64));
        insertRequest("NUTRITION", "APPROVAL", "SUCCESS", false, null, recipeId, false);

        jdbc.update("DELETE FROM recipes WHERE id = ?", recipeId);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM recipe_embeddings WHERE recipe_id = ?",
                Integer.class, recipeId)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ai_requests WHERE recipe_id = ?",
                Integer.class, recipeId)).isZero();
    }

    @Test
    void deletingUserRemovesTheirRequests() {
        insertRequest("MEAL_PLANNER", "USER", "AI_DRAFT", true, userId, null, true);

        jdbc.update("DELETE FROM users WHERE id = ?", userId);

        assertThat(jdbc.queryForObject("SELECT count(*) FROM ai_requests WHERE user_id = ?",
                Integer.class, userId)).isZero();
    }

    private static String vector768() {
        return "[" + "0.1,".repeat(767) + "0.1]";
    }
}
