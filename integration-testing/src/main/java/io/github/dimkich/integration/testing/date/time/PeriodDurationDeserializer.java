package io.github.dimkich.integration.testing.date.time;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

import java.io.IOException;

/**
 * Jackson deserializer for {@link PeriodDuration}.
 * <p>
 * Expects JSON string values in ISO-8601 format, for example:
 * <ul>
 *   <li>{@code "P1Y2M3DT4H5M6S"} – period and duration</li>
 *   <li>{@code "PT1H30M"} – duration only</li>
 *   <li>{@code "P7D"} – period only</li>
 * </ul>
 *
 * @see PeriodDuration#valueOf(CharSequence)
 * @see PeriodDuration
 */
public class PeriodDurationDeserializer extends StdScalarDeserializer<PeriodDuration> {

    /** Creates a deserializer for {@link PeriodDuration}. */
    public PeriodDurationDeserializer() {
        super(PeriodDuration.class);
    }

    /**
     * Deserializes a JSON string into a {@link PeriodDuration} using ISO-8601 format.
     *
     * @param p   the JSON parser
     * @param ctxt the deserialization context
     * @return the parsed period-duration
     * @throws IOException if an I/O error occurs or the value cannot be parsed
     */
    @Override
    public PeriodDuration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return PeriodDuration.valueOf(p.getText());
    }
}