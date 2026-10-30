package com.wikicook.ai.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Background AI work (embedding sync and reconcile, research R10) runs on its own virtual-thread executor,
 * so a slow provider never holds request threads.
 */
@Configuration(proxyBeanMethods = false)
@EnableAsync
@EnableScheduling
@EnableCaching
public class AiAsyncConfig {

    public static final String AI_TASK_EXECUTOR = "aiTaskExecutor";

    @Bean(AI_TASK_EXECUTOR)
    TaskExecutor aiTaskExecutor() {
        SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("ai-");
        executor.setVirtualThreads(true);
        return executor;
    }
}
