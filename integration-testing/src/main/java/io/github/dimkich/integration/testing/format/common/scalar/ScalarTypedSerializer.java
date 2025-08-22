package io.github.dimkich.integration.testing.format.common.scalar;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer;

import java.io.IOException;

public class ScalarTypedSerializer<T> extends StdScalarSerializer<T> {
    private JsonSerializer<T> serializer;

    public ScalarTypedSerializer(Class<T> t, JsonSerializer<T> serializer) {
        super(t);
        this.serializer = serializer;
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        this.serializer.serialize(value, gen, provider);
    }

    @Override
    @SuppressWarnings("unchecked")
    public JsonSerializer<T> replaceDelegatee(JsonSerializer<?> delegatee) {
        serializer = (JsonSerializer<T>)delegatee;
        return this;
    }

    @Override
    public JsonSerializer<?> getDelegatee() {
        return serializer;
    }
}
