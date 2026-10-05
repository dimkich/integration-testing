package io.github.dimkich.integration.testing.kafka.serde.serialization;

import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.kafka.support.KafkaHeaderMapper;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Header serializer converter delegating to a Spring Kafka {@link KafkaHeaderMapper}.
 */
@Getter
@SuppressWarnings("rawtypes")
public class SpringKafkaHeaderSerializer implements TestSerdeConverter<MultiValueMap, Headers, TestSerdeContext> {

    private final KafkaHeaderMapper delegate;

    private final Class<MultiValueMap> inputClass = MultiValueMap.class;

    private final Class<Headers> outputClass = Headers.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    public SpringKafkaHeaderSerializer(KafkaHeaderMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public Headers convert(MultiValueMap source, TestSerdeContext context) {
        Headers headers = new RecordHeaders();
        delegate.fromHeaders(new MessageHeaders(flattenSingleValues(source)), headers);
        return headers;
    }

    /**
     * Converts a multi-value map into the flat map the Spring mapper expects:
     * a single-element list is unwrapped to a scalar, a multi-element list is
     * kept as a {@code List} (so Spring encodes it as a JSON-array header, as it
     * does today). Nulls inside a multi-element list are preserved; a single
     * null value is skipped.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> flattenSingleValues(MultiValueMap source) {
        Map<String, Object> flat = new HashMap<>();
        if (source == null) {
            return flat;
        }
        for (Map.Entry<String, List<Object>> entry : ((Map<String, List<Object>>) source).entrySet()) {
            List<Object> values = entry.getValue();
            if (values.isEmpty()) {
                continue;
            }
            if (values.size() == 1) {
                if (values.get(0) != null) {
                    flat.put(entry.getKey(), values.get(0));
                }
            } else {
                flat.put(entry.getKey(), values.stream().toList());
            }
        }
        return flat;
    }
}
