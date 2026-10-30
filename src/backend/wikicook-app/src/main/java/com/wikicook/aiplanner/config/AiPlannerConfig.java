package com.wikicook.aiplanner.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers {@link AiPlannerProperties}. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AiPlannerProperties.class)
public class AiPlannerConfig {
}
