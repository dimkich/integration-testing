package io.github.dimkich.integration.testing.xml.map;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import lombok.RequiredArgsConstructor;

import java.io.IOException;

@RequiredArgsConstructor
public class TypeSerializerReplacedValue extends TypeSerializer {
    private final TypeSerializer typeSerializer;
    private final Object value;

    @Override
    public TypeSerializer forProperty(BeanProperty prop) {
        return typeSerializer.forProperty(prop);
    }

    @Override
    public JsonTypeInfo.As getTypeInclusion() {
        return typeSerializer.getTypeInclusion();
    }

    @Override
    public String getPropertyName() {
        return typeSerializer.getPropertyName();
    }

    @Override
    public TypeIdResolver getTypeIdResolver() {
        return typeSerializer.getTypeIdResolver();
    }

    @Override
    public WritableTypeId typeId(Object value, JsonToken valueShape) {
        return typeSerializer.typeId(this.value, valueShape);
    }

    @Override
    public WritableTypeId typeId(Object value, JsonToken valueShape, Object id) {
        return typeSerializer.typeId(this.value, valueShape, id);
    }

    @Override
    public WritableTypeId typeId(Object value, Class<?> typeForId, JsonToken valueShape) {
        return typeSerializer.typeId(this.value, typeForId, valueShape);
    }

    @Override
    public WritableTypeId writeTypePrefix(JsonGenerator g, WritableTypeId typeId) throws IOException {
        return typeSerializer.writeTypePrefix(g, typeId);
    }

    @Override
    public WritableTypeId writeTypeSuffix(JsonGenerator g, WritableTypeId typeId) throws IOException {
        return typeSerializer.writeTypeSuffix(g, typeId);
    }
}
