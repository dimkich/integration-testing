package io.github.dimkich.integration.testing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.Clock;

@Configuration
public class ClockConfig {
    @Bean
    @ConditionalOnMissingBean(Clock.class)
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    @ConditionalOnMissingBean(ClockService.class)
    ClockService clockService(Clock clock) {
        return new ClockService(clock);
    }

    @Bean
    @ConditionalOnClass(DateTimeProvider.class)
    TestsAwareDateTimeProvider testsAwareDateTimeProvider(ClockService clockService) {
        return new TestsAwareDateTimeProvider(clockService);
    }
}