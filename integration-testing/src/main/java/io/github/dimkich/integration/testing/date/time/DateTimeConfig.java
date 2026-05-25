package io.github.dimkich.integration.testing.date.time;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.dimkich.integration.testing.TestSetupModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Spring configuration for date-time control in integration tests.
 */
@Configuration
@Import({DateTimeService.class, LibFakeTimeNowSetter.class})
public class DateTimeConfig {
    @Bean
    TestSetupModule dateTimeModule() {
        return new TestSetupModule()
                .addSubTypes(PeriodDuration.class)
                .addJacksonModule(new SimpleModule()
                        .addSerializer(PeriodDuration.class, new PeriodDurationSerializer())
                        .addDeserializer(PeriodDuration.class, new PeriodDurationDeserializer()));
    }
}
