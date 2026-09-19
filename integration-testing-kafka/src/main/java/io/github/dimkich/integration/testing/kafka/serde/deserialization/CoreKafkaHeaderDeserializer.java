package io.github.dimkich.integration.testing.kafka.serde.deserialization;

import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeDeserializer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.Headers;
import org.springframework.util.MultiValueMap;

/**
 * Plain-style header deserializer for Core (context-free) providers.
 * Each Kafka header maps to a single value; duplicate keys accumulate into a
 * multi-value list in arrival order. No value-level heuristics are applied.
 * A header with a null value (tombstone header) is preserved as a {@code null}
 * value in the multi-value map.
 */
public class CoreKafkaHeaderDeserializer implements KafkaHeaderDeserializer {

    private final TestSerdeDeserializer<Object, SerdeContext> delegate;

    @SuppressWarnings("unchecked")
    public CoreKafkaHeaderDeserializer(TestSerdeDeserializer<?, ? extends SerdeContext> delegate) {
        this.delegate = (TestSerdeDeserializer<Object, SerdeContext>) delegate;
    }

    @Override
    public void deserialize(Headers source, MultiValueMap<String, Object> target) {
        if (source == null) {
            return;
        }
        for (Header header : source) {
            byte[] rawValue = header.value();
            if (rawValue == null) {
                target.add(header.key(), null);
                continue;
            }
            Object value = delegate.deserialize(rawValue, SerdeContext.EMPTY);
            if (value != null) {
                target.add(header.key(), value);
            }
        }
    }
}
