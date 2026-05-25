package io.github.dimkich.integration.testing.format.common.factory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer;
import com.fasterxml.jackson.databind.util.Converter;
import com.fasterxml.jackson.databind.util.NameTransformer;
import lombok.SneakyThrows;

import java.io.IOException;
import java.lang.reflect.Field;

public class TypedStdDelegatingSerializer extends StdDelegatingSerializer {
    private final static Field converterField;
    private final static Field delegateTypeField;

    private final boolean unwrapping;

    static {
        try {
            converterField = StdDelegatingSerializer.class.getDeclaredField("_converter");
            converterField.setAccessible(true);
            delegateTypeField = StdDelegatingSerializer.class.getDeclaredField("_delegateType");
            delegateTypeField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public TypedStdDelegatingSerializer(StdDelegatingSerializer serializer, boolean unwrapping) throws IllegalAccessException {
        super((Converter<Object, ?>) converterField.get(serializer),
                (JavaType) delegateTypeField.get(serializer), serializer.getDelegatee());
        this.unwrapping = unwrapping;
    }

    public TypedStdDelegatingSerializer(Converter<Object, ?> converter, JavaType delegateType) {
        super(converter, delegateType, null);
        unwrapping = false;
    }

    public TypedStdDelegatingSerializer(Converter<Object, ?> converter, JavaType delegateType, JsonSerializer<?> delegateSerializer) {
        super(converter, delegateType, delegateSerializer);
        unwrapping = false;
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        writeUnwrappingField(gen);
        super.serialize(value, gen, provider);
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer) throws IOException {
        writeUnwrappingField(gen);
        super.serializeWithType(value, gen, provider, new TypeSerializerReplacedValue(typeSer, value));
    }

    private void writeUnwrappingField(JsonGenerator gen) throws IOException {
        if (unwrapping) {
            gen.writeFieldName("");
        }
    }

    @Override
    public boolean isUnwrappingSerializer() {
        return true;
    }

    @Override
    @SneakyThrows
    public JsonSerializer<Object> unwrappingSerializer(NameTransformer unwrapper) {
        return new TypedStdDelegatingSerializer(this, true);
    }

    @Override
    protected JsonSerializer<Object> _findSerializer(Object value, SerializerProvider serializers) throws JsonMappingException {
        return serializers.findValueSerializer(_delegateType);
    }

    @Override
    protected StdDelegatingSerializer withDelegate(Converter<Object, ?> converter, JavaType delegateType, JsonSerializer<?> delegateSerializer) {
        return new TypedStdDelegatingSerializer(converter, delegateType, delegateSerializer);
    }
}
