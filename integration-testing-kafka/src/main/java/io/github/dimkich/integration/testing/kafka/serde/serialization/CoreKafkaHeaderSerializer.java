package io.github.dimkich.integration.testing.kafka.serde.serialization;

import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeSerializer;
import org.apache.kafka.common.header.Headers;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

/**
 * Plain-style header serializer for Core (context-free) providers.
 * <p>
 * Each value of a {@link MultiValueMap} entry becomes one Kafka header; a
 * multi-value entry expands into N headers sharing the same key. No
 * {@code spring_json_header_types} or other Spring-Kafka metadata is emitted.
 * A {@code null} value becomes a header with a null value (tombstone header).
 */
public class CoreKafkaHeaderSerializer implements KafkaHeaderSerializer {

    private final TestSerdeSerializer<Object, SerdeContext> delegate;

    @SuppressWarnings("unchecked")
    public CoreKafkaHeaderSerializer(TestSerdeSerializer<?, ? extends SerdeContext> delegate) {
        this.delegate = (TestSerdeSerializer<Object, SerdeContext>) delegate;
    }

    @Override
    public void serialize(MultiValueMap<String, Object> source, Headers target) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (Map.Entry<String, List<Object>> entry : source.entrySet()) {
            for (Object item : entry.getValue()) {
                if (item == null) {
                    target.add(entry.getKey(), null);
                    continue;
                }
                byte[] bytes = delegate.serialize(item, SerdeContext.EMPTY);
                if (bytes != null) {
                    target.add(entry.getKey(), bytes);
                }
            }
        }
    }
}
