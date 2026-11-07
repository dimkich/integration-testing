package io.github.dimkich.integration.testing.postgresql.serde;

import io.github.dimkich.integration.testing.postgresql.PostgresTemporalText;
import io.github.dimkich.integration.testing.postgresql.model.metadata.PostgresColumnSerdeContext;

import java.nio.ByteBuffer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;

/**
 * Converts PostgreSQL temporal text values using the zone from the column context.
 */
final class PostgresTemporalConverter extends PostgresConverter {

    @Override
    public Object convert(ByteBuffer input, PostgresColumnSerdeContext context) {
        String value = decode(input);
        if (value == null) {
            return null;
        }
        try {
            String normalized = value.replace(' ', 'T');
            return switch (context.getSqlTypeNameLower()) {
                case "timestamptz" -> OffsetDateTime.parse(
                                PostgresTemporalText.normalizeOffset(normalized))
                        .atZoneSameInstant(context.getZoneId())
                        .toLocalDateTime();
                case "timestamp" -> LocalDateTime.parse(normalized);
                case "date" -> LocalDate.parse(value);
                case "time" -> LocalTime.parse(value);
                case "timetz" -> OffsetTime.parse(PostgresTemporalText.normalizeOffset(normalized));
                default -> value;
            };
        } catch (DateTimeException e) {
            return value;
        }
    }
}
