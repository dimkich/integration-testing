package io.github.dimkich.integration.testing.format.util;

import com.fasterxml.jackson.databind.JsonSerializer;

import java.util.function.Function;

public class JacksonUtil {
    public static JsonSerializer<?> decorate(JsonSerializer<?> serializer,
                                             Function<JsonSerializer<?>, JsonSerializer<?>> converter) {
        JsonSerializer<?> previous = null;
        JsonSerializer<?> result = serializer;
        while (serializer != null) {
            JsonSerializer<?> ser = converter.apply(serializer);
            if (ser != null) {
                if (previous != null) {
                    previous.replaceDelegatee(ser);
                } else {
                    result = ser;
                }
                serializer = ser.getDelegatee().getDelegatee();
                previous = ser.getDelegatee();
            } else {
                previous = serializer;
                serializer = serializer.getDelegatee();
            }
        }
        return result;
    }
}
