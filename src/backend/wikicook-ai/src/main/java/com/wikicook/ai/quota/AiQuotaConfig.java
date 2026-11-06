package com.wikicook.ai.quota;

import com.wikicook.ai.config.AiProperties;
import java.time.Clock;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Quota beans; uses the application's {@link Clock} bean when there is one. */
@Configuration(proxyBeanMethods = false)
public class AiQuotaConfig {

    @Bean
    AiRequestRepository aiRequestRepository(NamedParameterJdbcTemplate jdbc) {
        return new AiRequestRepository(jdbc);
    }

    @Bean
    AiQuotaService aiQuotaService(AiRequestRepository repository, PlatformTransactionManager transactionManager,
                                  AiProperties properties, ObjectProvider<Clock> clock) {
        return new AiQuotaService(repository, new TransactionTemplate(transactionManager), properties.dailyCap(),
                clock.getIfAvailable(Clock::systemUTC));
    }
}
