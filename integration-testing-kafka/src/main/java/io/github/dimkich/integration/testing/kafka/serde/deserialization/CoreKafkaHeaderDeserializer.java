package io.github.dimkich.integration.testing.kafka.serde.deserialization;

import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.nio.ByteBuffer;

/**
 * Plain-style header deserializer converter wrapping a context-free converter.
 * Each Kafka header maps to a single value; duplicate keys accumulate into a
 * multi-value list in arrival order. No value-level heuristics are applied.
 * A header with a null value (tombstone header) is preserved as a {@code null}
 * value in the multi-value map.
 * <p>
 * Kafka headers store {@code byte[]}, so each value is wrapped into a {@link ByteBuffer}
 * at this boundary before it reaches the delegate.
 */
@Getter
@SuppressWarnings("rawtypes")
public class CoreKafkaHeaderDeserializer
        implements TestSerdeConverter<Headers, MultiValueMap, TestSerdeContext> {

    private final TestSerdeConverter<ByteBuffer, Object, TestSerdeContext> delegate;

    private final Class<Headers> inputClass = Headers.class;

    private final Class<MultiValueMap> outputClass = MultiValueMap.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    public CoreKafkaHeaderDeserializer(TestSerdeConverter<ByteBuffer, Object, TestSerdeContext> delegate) {
        this.delegate = delegate;
    }

    @Override
    public MultiValueMap convert(Headers headers, TestSerdeContext context) {
        MultiValueMap<String, Object> target = new LinkedMultiValueMap<>();
        if (headers == null) {
            return target;
        }
        for (Header header : headers) {
            byte[] rawValue = header.value();
            if (rawValue == null) {
                target.add(header.key(), null);
                continue;
            }
            Object value = delegate.convert(ByteBuffer.wrap(rawValue), TestSerdeContext.EMPTY);
            if (value != null) {
                target.add(header.key(), value);
            }
        }
        return target;
    }
}
