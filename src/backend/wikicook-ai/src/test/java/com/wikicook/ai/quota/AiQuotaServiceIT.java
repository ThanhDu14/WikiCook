package com.wikicook.ai.quota;

import static org.assertj.core.api.Assertions.assertThat;

import com.wikicook.ai.config.AiProperties;
import com.wikicook.ai.db.AbstractPostgresIT;
import com.wikicook.ai.testing.MutableClock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class AiQuotaServiceIT extends AbstractPostgresIT {

    private static final ZoneOffset VN = ZoneOffset.ofHours(7);
    private static final LocalDate WEEK = LocalDate.of(2026, 11, 2);
    /** 09:00 Vietnam time on Monday 2 November 2026. */
    private static final Instant NOW = OffsetDateTime.of(2026, 11, 2, 9, 0, 0, 0, VN).toInstant();

    private final MutableClock clock = new MutableClock(NOW);
    private final AiRequestRepository repository = new AiRequestRepository(namedJdbc);
    private long userId;
    private long recipeId;

    @BeforeEach
    void seed() {
        jdbc.update("DELETE FROM ai_requests");
        clock.set(NOW);
        userId = insertUser("quota-" + UUID.randomUUID() + "@wikicook.test");
        recipeId = insertRecipe();
    }

    /** total 10 → 7 rows for the meal planner, 3 for nutrition. */
    private AiQuotaService service(int total) {
        var cap = new AiProperties.DailyCap(total, 0.7, 0.3);
        return new AiQuotaService(repository, new TransactionTemplate(new DataSourceTransactionManager(DATA_SOURCE)),
                cap, clock);
    }

    private Reservation reservePlanner(AiQuotaService service) {
        return service.reserve(AiFeature.MEAL_PLANNER, AiTrigger.USER, userId, null, WEEK, 21, null);
    }

    @Test
    void reserveWritesProvisionalRow() {
        Reservation reservation = service(10).reserve(AiFeature.MEAL_PLANNER, AiTrigger.USER, userId, null, WEEK, 21,
                Map.of("servings", 2, "allergens", List.of("PEANUT")));

        assertThat(reservation.llmAllowed()).isTrue();
        AiRequest row = repository.findById(reservation.requestRowId()).orElseThrow();
        assertThat(row.feature()).isEqualTo(AiFeature.MEAL_PLANNER);
        assertThat(row.trigger()).isEqualTo(AiTrigger.USER);
        assertThat(row.outcome()).isEqualTo(AiOutcome.FAILED);
        assertThat(row.calledLlm()).isTrue();
        assertThat(row.counted()).isFalse();
        assertThat(row.userId()).isEqualTo(userId);
        assertThat(row.weekStart()).isEqualTo(WEEK);
        assertThat(row.requestedSlots()).isEqualTo(21);
        assertThat(row.createdAt()).isEqualTo(NOW);
        assertThat(row.constraints()).contains("\"servings\": 2").contains("PEANUT");
    }

    @Test
    void finishRecordsOutcome() {
        AiQuotaService service = service(10);
        long draft = reservePlanner(service).requestRowId();
        long cancelled = reservePlanner(service).requestRowId();

        service.finish(draft, AiOutcome.AI_DRAFT, true, 2, null);
        service.finish(cancelled, AiOutcome.CANCELLED, true, null, "CLIENT_LOST " + "x".repeat(200));

        AiRequest drafted = repository.findById(draft).orElseThrow();
        assertThat(drafted.outcome()).isEqualTo(AiOutcome.AI_DRAFT);
        assertThat(drafted.counted()).isTrue();
        assertThat(drafted.removedPicks()).isEqualTo(2);
        AiRequest lost = repository.findById(cancelled).orElseThrow();
        assertThat(lost.outcome()).isEqualTo(AiOutcome.CANCELLED);
        assertThat(lost.counted()).isFalse();
        assertThat(lost.calledLlm()).isTrue();
        assertThat(lost.reason()).startsWith("CLIENT_LOST").hasSize(100);
    }

    @Test
    void nutritionIsNeverCounted() {
        AiQuotaService service = service(10);
        long id = service.reserve(AiFeature.NUTRITION, AiTrigger.APPROVAL, null, recipeId, null, null, null)
                .requestRowId();

        service.finish(id, AiOutcome.SUCCESS, true, null, null);

        assertThat(repository.findById(id).orElseThrow().counted()).isFalse();
    }

    @Test
    void dailyCapUsesFeatureShareAndStartsAtMidnightVietnamTime() {
        AiQuotaService service = service(10);
        clock.set(OffsetDateTime.of(2026, 11, 1, 23, 50, 0, 0, VN).toInstant()); // yesterday
        for (int i = 0; i < 7; i++) {
            assertThat(reservePlanner(service).llmAllowed()).isTrue();
        }
        assertThat(reservePlanner(service).llmAllowed()).isFalse();

        clock.set(OffsetDateTime.of(2026, 11, 2, 0, 0, 0, 0, VN).toInstant()); // new day
        for (int i = 0; i < 7; i++) {
            assertThat(reservePlanner(service).llmAllowed()).isTrue();
        }
        Reservation over = reservePlanner(service);
        assertThat(over.llmAllowed()).isFalse();
        assertThat(repository.findById(over.requestRowId()).orElseThrow().calledLlm()).isFalse();

        // Nutrition has its own share (3) and is not affected by the planner.
        for (int i = 0; i < 3; i++) {
            assertThat(service.reserve(AiFeature.NUTRITION, AiTrigger.IMPORT, null, recipeId, null, null, null)
                    .llmAllowed()).isTrue();
        }
        assertThat(service.reserve(AiFeature.NUTRITION, AiTrigger.IMPORT, null, recipeId, null, null, null)
                .llmAllowed()).isFalse();
    }

    @Test
    void concurrentReservationsNeverExceedTheCap() throws Exception {
        AiQuotaService service = service(10);
        for (int i = 0; i < 6; i++) {
            reservePlanner(service); // one place left
        }
        int threads = 8;
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            Callable<Boolean> reserve = () -> {
                start.await();
                return reservePlanner(service).llmAllowed();
            };
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(reserve));
            }
            start.countDown();
            int allowed = 0;
            for (Future<Boolean> result : results) {
                allowed += result.get() ? 1 : 0;
            }
            assertThat(allowed).isEqualTo(1);
        }
    }

    private void insertCounted(long user, Instant createdAt) {
        jdbc.update("""
                INSERT INTO ai_requests (feature, trigger, outcome, called_llm, counted, user_id, week_start, created_at)
                VALUES ('MEAL_PLANNER', 'USER', 'AI_DRAFT', true, true, ?, ?, ?)
                """, user, WEEK, java.sql.Timestamp.from(createdAt));
    }

    @Test
    void userAllowanceCountsOnlyCountedRowsInRollingHour() {
        AiQuotaService service = service(10);
        insertCounted(userId, NOW.minus(Duration.ofMinutes(61))); // outside the window
        insertCounted(userId, NOW.minus(Duration.ofMinutes(59)));
        insertCounted(insertUser("other-" + UUID.randomUUID() + "@wikicook.test"), NOW.minus(Duration.ofMinutes(5)));
        service.finish(reservePlanner(service).requestRowId(), AiOutcome.AUTO_DRAFT, false, null, null);

        UserAllowance allowance = service.userAllowance(userId);

        assertThat(allowance.remaining()).isEqualTo(4);
        assertThat(allowance.nextAvailableAt()).isNull();
    }

    @Test
    void userAllowanceAtLimitSaysWhenTheOldestExpires() {
        AiQuotaService service = service(10);
        Instant oldest = NOW.minus(Duration.ofMinutes(50));
        insertCounted(userId, oldest);
        for (int i = 1; i <= 4; i++) {
            insertCounted(userId, NOW.minus(Duration.ofMinutes(10L * i)));
        }

        UserAllowance allowance = service.userAllowance(userId);

        assertThat(allowance.remaining()).isZero();
        assertThat(allowance.nextAvailableAt()).isEqualTo(oldest.plus(Duration.ofMinutes(60)));
    }
}
