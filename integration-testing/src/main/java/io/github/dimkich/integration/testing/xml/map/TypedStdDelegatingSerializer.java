package io.github.dimkich.integration.testing.xml.map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.StdDelegatingSerializer;
import com.fasterxml.jackson.databind.util.Converter;

import java.io.IOException;
import java.lang.reflect.Field;

public class TypedStdDelegatingSerializer extends StdDelegatingSerializer {
    private final static Field converterField;
    private final static Field delegateTypeField;

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

    public TypedStdDelegatingSerializer(StdDelegatingSerializer serializer) throws IllegalAccessException {
        super((Converter<Object, ?>) converterField.get(serializer),
                (JavaType) delegateTypeField.get(serializer), serializer.getDelegatee());
    }

    public TypedStdDelegatingSerializer(Converter<Object, ?> converter, JavaType delegateType) {
        super(converter, delegateType, null);
    }

    public TypedStdDelegatingSerializer(Converter<Object, ?> converter, JavaType delegateType, JsonSerializer<?> delegateSerializer) {
        super(converter, delegateType, delegateSerializer);
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer) throws IOException {
        super.serializeWithType(value, gen, provider, new TypeSerializerReplacedValue(typeSer, value));
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
