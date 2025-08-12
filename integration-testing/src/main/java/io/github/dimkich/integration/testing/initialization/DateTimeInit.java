package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.TestInit;
import io.github.dimkich.integration.testing.date.time.DateTimeService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

@Getter
@Setter
@ToString
public class DateTimeInit extends TestInit {
    @JacksonXmlProperty(isAttribute = true)
    private ZonedDateTime dateTime;
    @JacksonXmlProperty(isAttribute = true)
    private Duration addDuration;

    @RequiredArgsConstructor
    public static class Init implements Initializer<DateTimeInit> {
        private final DateTimeService dateTimeService;

        private ZonedDateTime dateTime;
        private Duration duration;

        @Override
        public Class<DateTimeInit> getTestInitClass() {
            return DateTimeInit.class;
        }

        @Override
        public Integer getOrder() {
            return 0;
        }

        @Override
        public void init(Stream<DateTimeInit> inits) {
            dateTime = null;
            duration = null;
            inits.forEach(init -> {
                if (init.getDateTime() != null) {
                    dateTime = init.getDateTime();
                }
                if (init.getAddDuration() != null) {
                    duration = init.getAddDuration();
                }
            });
            if (dateTime != null) {
                dateTimeService.setNow(dateTime);
            }
            if (duration != null) {
                dateTimeService.addDuration(duration);
            }
        }
    }
}
