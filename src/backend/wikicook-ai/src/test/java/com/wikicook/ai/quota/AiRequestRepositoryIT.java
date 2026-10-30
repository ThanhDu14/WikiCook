package com.wikicook.ai.quota;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.ai.db.AbstractPostgresIT;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiRequestRepositoryIT extends AbstractPostgresIT {

    private static final Instant NOW = Instant.parse("2026-11-02T02:00:00Z");

    private final AiRequestRepository repository = new AiRequestRepository(namedJdbc);
    private long userId;
    private long recipeId;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM ai_requests");
        userId = insertUser("purge-" + UUID.randomUUID() + "@wikicook.test");
        recipeId = insertRecipe();
    }

    private void insert(String feature, Instant createdAt) {
        jdbc.update("""
                INSERT INTO ai_requests (feature, trigger, outcome, called_llm, counted, user_id, recipe_id, week_start,
                                         created_at)
                VALUES (?, 'USER', 'FAILED', true, false, ?, ?, DATE '2026-11-02', ?)
                """, feature, userId, recipeId, Timestamp.from(createdAt));
    }

    @Test
    void deletesOnlyOldRowsOfTheFeature() {
        insert("MEAL_PLANNER", NOW.minus(Duration.ofDays(91)));
        insert("MEAL_PLANNER", NOW.minus(Duration.ofDays(89)));
        insert("NUTRITION", NOW.minus(Duration.ofDays(200))); // 090 keeps its history

        int deleted = repository.deleteOlderThan(AiFeature.MEAL_PLANNER, NOW.minus(Duration.ofDays(90)));

        assertThat(deleted).isEqualTo(1);
        assertThat(jdbc.queryForList("SELECT feature FROM ai_requests ORDER BY feature", String.class))
                .containsExactly("MEAL_PLANNER", "NUTRITION");
    }
}
