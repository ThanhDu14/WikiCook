package com.wikicook.ai.quota;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

/** {@code ai_requests} over JDBC: this module has no JPA (tasks.md Path Conventions). */
public class AiRequestRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public AiRequestRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** @return the new row's id */
    public long insert(AiRequest request) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("feature", request.feature().name())
                .addValue("userId", request.userId(), Types.BIGINT)
                .addValue("recipeId", request.recipeId(), Types.BIGINT)
                .addValue("actorId", request.actorId(), Types.BIGINT)
                .addValue("trigger", request.trigger().name())
                .addValue("outcome", request.outcome().name())
                .addValue("calledLlm", request.calledLlm())
                .addValue("counted", request.counted())
                .addValue("weekStart", request.weekStart(), Types.DATE)
                .addValue("requestedSlots", request.requestedSlots(), Types.SMALLINT)
                .addValue("removedPicks", request.removedPicks(), Types.SMALLINT)
                .addValue("constraints", request.constraints(), Types.VARCHAR)
                .addValue("reason", request.reason(), Types.VARCHAR)
                .addValue("createdAt", request.createdAt().atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE);
        return jdbc.queryForObject("""
                INSERT INTO ai_requests (feature, user_id, recipe_id, actor_id, trigger, outcome, called_llm, counted,
                                         week_start, requested_slots, removed_picks, constraints, reason, created_at)
                VALUES (:feature, :userId, :recipeId, :actorId, :trigger, :outcome, :calledLlm, :counted,
                        :weekStart, :requestedSlots, :removedPicks, CAST(:constraints AS jsonb), :reason, :createdAt)
                RETURNING id
                """, params, Long.class);
    }

    /** Sets the final outcome; {@code counted} follows from feature and outcome, as the schema CHECK demands. */
    public void finish(long id, AiOutcome outcome, boolean calledLlm, Integer removedPicks, String reason) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("outcome", outcome.name())
                .addValue("calledLlm", calledLlm)
                .addValue("removedPicks", removedPicks, Types.SMALLINT)
                .addValue("reason", reason, Types.VARCHAR);
        int updated = jdbc.update("""
                UPDATE ai_requests
                   SET outcome = :outcome,
                       called_llm = :calledLlm,
                       counted = (feature = 'MEAL_PLANNER' AND :outcome = 'AI_DRAFT'),
                       removed_picks = :removedPicks,
                       reason = :reason
                 WHERE id = :id
                """, params);
        if (updated == 0) {
            throw new IllegalArgumentException("No ai_requests row " + id);
        }
    }

    public Optional<AiRequest> findById(long id) {
        return jdbc.query("SELECT * FROM ai_requests WHERE id = :id", new MapSqlParameterSource("id", id),
                (rs, rowNum) -> map(rs)).stream().findFirst();
    }

    /** Serialises callers per feature until the transaction ends; call inside a transaction. */
    public void lockFeature(AiFeature feature) {
        jdbc.query("SELECT pg_advisory_xact_lock(hashtext(:feature))",
                new MapSqlParameterSource("feature", feature.name()), (ResultSetExtractor<Void>) rs -> null);
    }

    /** Rows of {@code feature} that called (or reserved a call to) the LLM since {@code since}. */
    public int countCalledLlmSince(AiFeature feature, Instant since) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("feature", feature.name())
                .addValue("since", since.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE);
        return jdbc.queryForObject(
                "SELECT count(*) FROM ai_requests WHERE feature = :feature AND called_llm AND created_at >= :since",
                params, Integer.class);
    }

    /** Retention: meal planner rows are kept 90 days; nutrition rows stay as recipe history (090). */
    public int deleteOlderThan(AiFeature feature, Instant before) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("feature", feature.name())
                .addValue("before", before.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE);
        return jdbc.update("DELETE FROM ai_requests WHERE feature = :feature AND created_at < :before", params);
    }

    /** Counted meal planner rows of a user after {@code after}, and the oldest of them. */
    public CountedWindow countedAfter(long userId, Instant after) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("userId", userId)
                .addValue("after", after.atOffset(ZoneOffset.UTC), Types.TIMESTAMP_WITH_TIMEZONE);
        return jdbc.queryForObject("""
                SELECT count(*) AS n, min(created_at) AS oldest
                  FROM ai_requests
                 WHERE feature = 'MEAL_PLANNER' AND user_id = :userId AND counted AND created_at > :after
                """, params, (rs, rowNum) -> new CountedWindow(rs.getInt("n"), instant(rs, "oldest")));
    }

    /** @param oldest {@code null} when {@code count} is 0 */
    public record CountedWindow(int count, Instant oldest) {
    }

    private static AiRequest map(ResultSet rs) throws SQLException {
        return new AiRequest(
                rs.getLong("id"),
                AiFeature.valueOf(rs.getString("feature")),
                rs.getObject("user_id", Long.class),
                rs.getObject("recipe_id", Long.class),
                rs.getObject("actor_id", Long.class),
                AiTrigger.valueOf(rs.getString("trigger")),
                AiOutcome.valueOf(rs.getString("outcome")),
                rs.getBoolean("called_llm"),
                rs.getBoolean("counted"),
                rs.getObject("week_start", LocalDate.class),
                rs.getObject("requested_slots", Integer.class),
                rs.getObject("removed_picks", Integer.class),
                rs.getString("constraints"),
                rs.getString("reason"),
                instant(rs, "created_at"));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        OffsetDateTime value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }
}
