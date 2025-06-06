package io.github.dimkich.integration.testing.date.time;

import io.github.dimkich.integration.testing.NowSetter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;

@RequiredArgsConstructor
public class DateTimeService {
    private final List<NowSetter> nowSetters;
    @Getter
    private ZonedDateTime dateTime;

    public void setNow(ZonedDateTime dateTime) {
        this.dateTime = dateTime;
        nowSetters.forEach(s -> s.setNow(dateTime));
    }

    public void addDuration(Duration duration) {
        if (dateTime == null) {
            throw new RuntimeException("Service is not initialized");
        }
        setNow(dateTime.plus(duration));
    }
}
