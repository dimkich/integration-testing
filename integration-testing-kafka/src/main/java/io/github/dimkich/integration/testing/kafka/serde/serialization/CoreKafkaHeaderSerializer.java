package io.github.dimkich.integration.testing.kafka.serde.serialization;

import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.util.MultiValueMap;

import java.util.List;
import java.util.Map;

/**
 * Plain-style header serializer converter wrapping a context-free converter.
 * <p>
 * Each value of a {@link MultiValueMap} entry becomes one Kafka header; a
 * multi-value entry expands into N headers sharing the same key. No
 * {@code spring_json_header_types} or other Spring-Kafka metadata is emitted.
 * A {@code null} value becomes a header with a null value (tombstone header).
 */
@Getter
@SuppressWarnings("rawtypes")
public class CoreKafkaHeaderSerializer implements TestSerdeConverter<MultiValueMap, Headers, TestSerdeContext> {

    private final TestSerdeConverter<Object, byte[], TestSerdeContext> delegate;

    private final Class<MultiValueMap> inputClass = MultiValueMap.class;

    private final Class<Headers> outputClass = Headers.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    public CoreKafkaHeaderSerializer(TestSerdeConverter<Object, byte[], TestSerdeContext> delegate) {
        this.delegate = delegate;
    }

    @Override
    public Headers convert(MultiValueMap map, TestSerdeContext context) {
        Headers headers = new RecordHeaders();
        if (map == null || map.isEmpty()) {
            return headers;
        }
        for (Map.Entry<String, List<Object>> entry : entries(map).entrySet()) {
            for (Object item : entry.getValue()) {
                if (item == null) {
                    headers.add(entry.getKey(), null);
                    continue;
                }
                byte[] bytes = delegate.convert(item, TestSerdeContext.EMPTY);
                if (bytes != null) {
                    headers.add(entry.getKey(), bytes);
                }
            }
        }
        return headers;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, List<Object>> entries(MultiValueMap map) {
        return (Map<String, List<Object>>) map;
    }
}
