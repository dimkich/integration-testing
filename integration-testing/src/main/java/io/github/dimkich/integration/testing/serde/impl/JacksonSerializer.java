package io.github.dimkich.integration.testing.serde.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeSerializer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * Serializes values to bytes with a Jackson {@link ObjectMapper}.
 *
 * @param <T> the value type
 */
@RequiredArgsConstructor
public class JacksonSerializer<T> implements TestSerdeSerializer<T, SerdeContext> {
    private final ObjectMapper objectMapper;

    @Override
    public Class<SerdeContext> getContextClass() {
        return SerdeContext.class;
    }

    @Override
    @SneakyThrows
    public byte[] serialize(T data, SerdeContext context) {
        return data == null ? null : objectMapper.writeValueAsBytes(data);
    }
}
