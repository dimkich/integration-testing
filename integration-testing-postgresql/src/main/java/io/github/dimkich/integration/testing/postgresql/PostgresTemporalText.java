package io.github.dimkich.integration.testing.postgresql;

import lombok.experimental.UtilityClass;

/**
 * Normalizes PostgreSQL temporal text offsets to the ISO form accepted by {@code java.time}.
 * PostgreSQL may emit a whole-hour offset such as {@code +04}, while {@link java.time.OffsetTime}
 * expects minutes as in {@code +04:00}.
 */
@UtilityClass
public class PostgresTemporalText {

    public static String normalizeOffset(String value) {
        int sign = Math.max(value.lastIndexOf('+'), value.lastIndexOf('-'));
        if (sign < 0 || value.indexOf(':') < 0) {
            return value;
        }
        int suffixLength = value.length() - sign - 1;
        if (suffixLength == 2) {
            return value + ":00";
        }
        if (suffixLength == 4 && value.indexOf(':', sign + 1) < 0) {
            return value.substring(0, sign + 3) + ":" + value.substring(sign + 3);
        }
        return value;
    }
}
