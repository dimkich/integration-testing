package io.github.dimkich.integration.testing.storage.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.github.dimkich.integration.testing.storage.dto.StructuredValue;

import java.io.IOException;

/**
 * Serializes a {@link StructuredValue} as an object with one property and declares the object
 * format through a format visitor, so the difference engine treats the value as structured.
 */
public class StructuredValueSerializer extends StdSerializer<StructuredValue> {

    public StructuredValueSerializer() {
        super(StructuredValue.class);
    }

    @Override
    public void serialize(StructuredValue value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("payload", value.getPayload());
        gen.writeEndObject();
    }

    @Override
    public void serializeWithType(StructuredValue value, JsonGenerator gen, SerializerProvider provider,
                                  TypeSerializer typeSer) throws IOException {
        WritableTypeId typeId = typeSer.writeTypePrefix(gen, typeSer.typeId(value, JsonToken.START_OBJECT));
        gen.writeStringField("payload", value.getPayload());
        typeSer.writeTypeSuffix(gen, typeId);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType type) throws JsonMappingException {
        visitor.expectObjectFormat(type);
    }
}
