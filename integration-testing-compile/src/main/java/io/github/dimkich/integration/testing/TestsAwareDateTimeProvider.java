package io.github.dimkich.integration.testing;

import lombok.RequiredArgsConstructor;
import org.springframework.data.auditing.DateTimeProvider;

import java.time.temporal.TemporalAccessor;
import java.util.Optional;

@RequiredArgsConstructor
public class TestsAwareDateTimeProvider implements DateTimeProvider {
    private final ClockService clockService;

    @Override
    public Optional<TemporalAccessor> getNow() {
        return Optional.of(clockService.getZonedDateTime());
    }
}
