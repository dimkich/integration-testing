package io.github.dimkich.integration.testing.format.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonSerializer;
import lombok.SneakyThrows;

import java.util.function.Function;

public class JacksonUtils {
    public static String getCurrentName(JsonGenerator gen) {
        JsonStreamContext context = gen.getOutputContext();
        while (context.getCurrentName() == null) {
            context = context.getParent();
        }
        return context.getCurrentName();
    }

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

    // for debug purpose
    @SneakyThrows
    public static void printCurrentToken(JsonParser p) {
        int i = 0;
        JsonStreamContext context = p.getParsingContext();
        if (context == null) {
            return;
        }
        while (context.getParent() != null) {
            context = context.getParent();
            i++;
        }
        if (p.currentToken() == JsonToken.START_OBJECT || p.currentToken() == JsonToken.START_ARRAY) {
            i--;
        }
        System.out.print(" ".repeat(i * 4) + p.currentToken());
        if (p.currentToken() == JsonToken.FIELD_NAME) {
            System.out.print(" " + p.currentName());
        }
        if (p.currentToken() == JsonToken.VALUE_STRING) {
            System.out.print(" " + p.getText());
        }
        if (p.currentToken() == JsonToken.VALUE_NULL) {
            System.out.print(" null");
        }
        System.out.println();
    }
}
