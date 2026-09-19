package io.github.dimkich.integration.testing.kafka.serde.serialization;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.support.KafkaHeaderMapper;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link KafkaHeaderSerializer} delegating to a Spring Kafka {@link KafkaHeaderMapper}.
 */
@RequiredArgsConstructor
public class SpringKafkaHeaderSerializer implements KafkaHeaderSerializer {

    private final KafkaHeaderMapper delegate;

    @Override
    public void serialize(MultiValueMap<String, Object> source, Headers target) {
        delegate.fromHeaders(new MessageHeaders(flattenSingleValues(source)), target);
    }

    /**
     * Converts a multi-value map into the flat map the Spring mapper expects:
     * a single-element list is unwrapped to a scalar, a multi-element list is
     * kept as a {@code List} (so Spring encodes it as a JSON-array header, as it
     * does today). Nulls inside a multi-element list are preserved; a single
     * null value is skipped.
     */
    public static Map<String, Object> flattenSingleValues(MultiValueMap<String, Object> source) {
        Map<String, Object> flat = new HashMap<>();
        if (source == null) {
            return flat;
        }
        for (Map.Entry<String, List<Object>> entry : source.entrySet()) {
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
