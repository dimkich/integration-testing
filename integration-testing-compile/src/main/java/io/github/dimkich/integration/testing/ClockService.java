package io.github.dimkich.integration.testing;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.sql.Timestamp;
import java.time.*;

@Getter
@AllArgsConstructor
public class ClockService {
    protected Clock clock;

    public LocalDateTime getLocalDateTime() {
        return LocalDateTime.now(clock);
    }

    public ZonedDateTime getZonedDateTime() {
        return ZonedDateTime.now(clock);
    }

    public Instant getInstant() {
        return clock.instant();
    }

    public LocalDate getLocalDate() {
        return LocalDate.now(clock);
    }

    public LocalTime getLocalTime() {
        return LocalTime.now(clock);
    }

    public LocalDate getLocalDate(ZoneId zone) {
        return getZonedDateTime().withZoneSameInstant(zone).toLocalDate();
    }

    public long getEpochSecond() {
        return clock.instant().getEpochSecond();
    }

    public long getEpochMillis() {
        return clock.millis();
    }

    public LocalDateTime toLocalDateTime(Long milli) {
        if (milli == null) {
            return null;
        }
        return Instant.ofEpochMilli(milli).atZone(clock.getZone()).toLocalDateTime();
    }

    public LocalDate toLocalDate(Long milli) {
        if (milli == null) {
            return null;
        }
        return Instant.ofEpochMilli(milli).atZone(clock.getZone()).toLocalDate();
    }

    public Long toEpochMilli(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(clock.getZone()).toInstant().toEpochMilli();
    }

    public Long toEpochSeconds(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(clock.getZone()).toInstant().getEpochSecond();
    }

    public LocalDateTime fromEpochSeconds(Long seconds) {
        if (seconds == null) {
            return null;
        }
        return Instant.ofEpochSecond(seconds).atZone(clock.getZone()).toLocalDateTime();
    }

    public Timestamp toTimestamp(Long milli) {
        return Timestamp.valueOf(toLocalDateTime(milli));
    }

    public void sleep(long millis) throws InterruptedException {
        Thread.sleep(millis);
    }

    public boolean isInInterval(LocalTime start, LocalTime end) {
        LocalTime now = getLocalTime();
        if (start.isBefore(end)) {
            if (now.isBefore(start)) {
                return false;
            }
        } else if (now.isAfter(start)) {
            return true;
        }
        return now.isBefore(end);
    }
}
