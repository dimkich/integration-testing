package io.github.dimkich.integration.testing.date.time;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;

/**
 * Jackson serializer for {@link PeriodDuration}.
 * <p>
 * Serializes a {@link PeriodDuration} to a JSON string using its ISO-8601 representation
 * (e.g. {@code "P1Y2M3DT4H5M6S"}, {@code "PT1H30M"}, {@code "PT0S"} for zero).
 *
 * @see PeriodDuration
 * @see PeriodDurationDeserializer
 */
public class PeriodDurationSerializer extends StdScalarSerializer<PeriodDuration> {

    /**
     * Creates a new serializer for {@link PeriodDuration}.
     */
    public PeriodDurationSerializer() {
        super(PeriodDuration.class);
    }

    /**
     * Serializes the given {@link PeriodDuration} to a JSON string.
     *
     * @param pd                 the period-duration to serialize
     * @param jsonGenerator      the JSON generator to write to
     * @param serializerProvider the serializer provider
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void serialize(PeriodDuration pd, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(pd.toString());
    }
}