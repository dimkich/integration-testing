package io.github.dimkich.integration.testing.format.common.scalar;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.github.dimkich.integration.testing.format.util.JacksonUtils;
import lombok.RequiredArgsConstructor;

import java.util.Set;

@RequiredArgsConstructor
public class ScalarTypeSerializerModifier extends BeanSerializerModifier {
    private final Set<Class<?>> classes;

    @Override
    @SuppressWarnings("unchecked")
    public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc,
                                              JsonSerializer<?> serializer) {
        serializer = JacksonUtils.decorate(serializer, s -> {
            if (s instanceof StdSerializer<?>) {
                StdSerializer<Object> stdSerializer = (StdSerializer<Object>) s;
                if (classes.contains(stdSerializer.handledType())) {
                    return new ScalarTypedSerializer<>(stdSerializer.handledType(), stdSerializer);
                }
            }
            return null;
        });
        return serializer;
    }
}
