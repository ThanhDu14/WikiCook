package com.wikicook.aiplanner.support;

import com.wikicook.ai.testing.FakeEmbeddingClient;
import com.wikicook.ai.testing.FakeLlmClient;
import com.wikicook.ai.testing.MutableClock;
import java.time.Instant;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Beans shared by the AI planner ITs of US3–US5: importing the same class with the same properties lets them share
 * one cached context, so one pool of database connections. The clock starts at "now"; a test that moves it sets it
 * back in {@code @BeforeEach}. The fake LLM keeps every request of the context: compare counts, not totals.
 */
@TestConfiguration
public class AiPlannerItBeans {

    @Bean
    @Primary
    public FakeLlmClient fakeLlmClient() {
        return new FakeLlmClient();
    }

    @Bean
    @Primary
    public FakeEmbeddingClient fakeEmbeddingClient() {
        return new FakeEmbeddingClient(768);
    }

    @Bean
    @Primary
    public MutableClock mutableClock() {
        return new MutableClock(Instant.now());
    }
}
