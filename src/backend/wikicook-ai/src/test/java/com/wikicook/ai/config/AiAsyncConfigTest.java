package com.wikicook.ai.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cache.CacheManager;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.config.ScheduledTaskHolder;

class AiAsyncConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(CacheAutoConfiguration.class))
            .withUserConfiguration(AiAsyncConfig.class, Worker.class);

    @Test
    void asyncWorkRunsOnVirtualThreadsOfTheAiExecutor() {
        runner.run(context -> {
            assertThat(context.getBean(AiAsyncConfig.AI_TASK_EXECUTOR, TaskExecutor.class)).isNotNull();
            Thread thread = context.getBean(Worker.class).currentThread().join();
            assertThat(thread.isVirtual()).isTrue();
            assertThat(thread.getName()).startsWith("ai-");
        });
    }

    @Test
    void enablesSchedulingAndCaching() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(ScheduledTaskHolder.class);
            assertThat(context).hasSingleBean(CacheManager.class);
        });
    }

    static class Worker {

        @Async(AiAsyncConfig.AI_TASK_EXECUTOR)
        public CompletableFuture<Thread> currentThread() {
            return CompletableFuture.completedFuture(Thread.currentThread());
        }
    }
}
