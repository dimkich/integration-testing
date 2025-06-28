package io.github.dimkich.integration.testing.date.time;

import io.github.dimkich.integration.testing.ClockService;
import io.github.dimkich.integration.testing.NowSetter;

import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;

public class TestClockService extends ClockService implements NowSetter {
    public TestClockService() {
        super(Clock.systemUTC());
    }

    public void addDuration(Duration duration) {
        clock = Clock.fixed(clock.instant().plus(duration), clock.getZone());
    }

    @Override
    public void setNow(ZonedDateTime dateTime) {
        clock = Clock.fixed(dateTime.toInstant(), dateTime.getZone());
    }

    public void setClock(Clock clock) {
        this.clock = clock;
    }
}
