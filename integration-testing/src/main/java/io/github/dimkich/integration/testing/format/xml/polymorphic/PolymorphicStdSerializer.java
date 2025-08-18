package io.github.dimkich.integration.testing.format.xml.polymorphic;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import lombok.SneakyThrows;

import java.io.IOException;
import java.lang.reflect.Field;

public class PolymorphicStdSerializer<T> extends StdSerializer<T> {
    private final static Field nextIsAttributeField;

    static {
        try {
            nextIsAttributeField = ToXmlGenerator.class.getDeclaredField("_nextIsAttribute");
            nextIsAttributeField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    private final StdSerializer<T> stdSerializer;

    @SneakyThrows
    public PolymorphicStdSerializer(StdSerializer<T> src) {
        super(src);
        stdSerializer = src;
    }

    @Override
    public void serialize(T value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        stdSerializer.serialize(value, gen, provider);
    }

    @Override
    @SneakyThrows
    public void serializeWithType(T value, JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) {
        ToXmlGenerator generator = (ToXmlGenerator) gen;

        WritableTypeId typeId = typeSer.typeId(value, JsonToken.START_OBJECT);
        boolean nextIsAttribute = (Boolean) nextIsAttributeField.get(generator);
        if (!nextIsAttribute) {
            generator.setNextIsAttribute(true);
            typeSer.writeTypePrefix(gen, typeId);
            generator.setNextIsAttribute(false);

            generator.setNextIsUnwrapped(true);
            gen.writeFieldName("");
        }
        serialize(value, gen, serializers);
        if (!nextIsAttribute) {
            generator.setNextIsUnwrapped(false);
            typeSer.writeTypeSuffix(gen, typeId);
        }
    }
}
