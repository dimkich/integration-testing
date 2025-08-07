package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.TestCaseInit;
import io.github.dimkich.integration.testing.date.time.DateTimeService;
import lombok.*;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Collection;

@Getter
@Setter
@ToString
public class DateTimeInit extends TestCaseInit {
    @JacksonXmlProperty(isAttribute = true)
    private ZonedDateTime dateTime;
    @JacksonXmlProperty(isAttribute = true)
    private Duration addDuration;

    @RequiredArgsConstructor
    public static class Init implements Initializer<DateTimeInit> {
        private final DateTimeService dateTimeService;

        @Override
        public Class<DateTimeInit> getTestCaseInitClass() {
            return DateTimeInit.class;
        }

        @Override
        public Integer getOrder() {
            return 0;
        }

        @Override
        public void init(Collection<DateTimeInit> inits) {
            ZonedDateTime dateTime = null;
            Duration duration = null;
            for (DateTimeInit init : inits) {
                if (init.getDateTime() != null) {
                    dateTime = init.getDateTime();
                }
                if (init.getAddDuration() != null) {
                    duration = init.getAddDuration();
                }
            }
            if (dateTime != null) {
                dateTimeService.setNow(dateTime);
            }
            if (duration != null) {
                dateTimeService.addDuration(duration);
            }
        }
    }
}
