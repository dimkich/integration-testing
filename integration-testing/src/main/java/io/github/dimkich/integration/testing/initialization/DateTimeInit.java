package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.TestCaseInit;
import io.github.dimkich.integration.testing.date.time.DateTimeService;
import lombok.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZonedDateTime;

@Getter
@Setter
@ToString
public class DateTimeInit extends TestCaseInit {
    @JacksonXmlProperty(isAttribute = true)
    private ZonedDateTime dateTime;
    @JacksonXmlProperty(isAttribute = true)
    private Duration addDuration;

    @Override
    public Integer getOrder() {
        return 0;
    }

    @Component
    @RequiredArgsConstructor
    public static class Initializer implements TestCaseInitializer<DateTimeInit> {
        private final DateTimeService dateTimeService;

        @Override
        public Class<DateTimeInit> getTestCaseInitClass() {
            return DateTimeInit.class;
        }

        @Override
        public void init(DateTimeInit init) {
            if (init.getDateTime() != null) {
                dateTimeService.setNow(init.getDateTime());
            }
            if (init.getAddDuration() != null) {
                dateTimeService.addDuration(init.getAddDuration());
            }
        }
    }
}
