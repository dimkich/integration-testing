package io.github.dimkich.integration.testing.storage.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.github.dimkich.integration.testing.storage.dto.StructuredValueWithoutVisitor;

import java.io.IOException;

/**
 * Serializes a {@link StructuredValueWithoutVisitor} as an object with one property but does not
 * declare the object format through a format visitor, so the difference engine has to fall back to
 * the serializer shape.
 */
public class StructuredValueWithoutVisitorSerializer extends StdSerializer<StructuredValueWithoutVisitor> {

    public StructuredValueWithoutVisitorSerializer() {
        super(StructuredValueWithoutVisitor.class);
    }

    @Override
    public void serialize(StructuredValueWithoutVisitor value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        gen.writeStartObject();
        gen.writeStringField("payload", value.getPayload());
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(StructuredValueWithoutVisitor value, JsonGenerator gen, SerializerProvider provider,
                                  TypeSerializer typeSer) throws IOException {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, typeSer.typeId(value, JsonToken.START_OBJECT));
        gen.writeStringField("payload", value.getPayload());
        typeSer.writeTypeSuffix(gen, typeId);
    }
}
