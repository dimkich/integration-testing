package io.github.dimkich.integration.testing.kafka.serde.deserialization;

import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.support.KafkaHeaderMapper;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Header deserializer converter using a {@link KafkaHeaderMapper}, typically
 * {@code DefaultKafkaHeaderMapper}.
 *
 * <p>The mapper handles duplicate keys and Spring Kafka's special headers
 * ({@code __TypeId__}, {@code __KeyTypeId__}, {@code __ContentTypeId__}) according to its own
 * contract — this is intentional and mirrors what the SUT does in production. If you need
 * generic duplicate collection without Spring-Kafka semantics, use
 * {@link CoreKafkaHeaderDeserializer} instead.
 */
@Getter
@SuppressWarnings("rawtypes")
public class SpringKafkaHeaderDeserializer implements TestSerdeConverter<Headers, MultiValueMap, TestSerdeContext> {

    private final KafkaHeaderMapper delegate;

    private final Class<Headers> inputClass = Headers.class;

    private final Class<MultiValueMap> outputClass = MultiValueMap.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    public SpringKafkaHeaderDeserializer(KafkaHeaderMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public MultiValueMap convert(Headers headers, TestSerdeContext context) {
        MultiValueMap<String, Object> target = new LinkedMultiValueMap<>();
        if (headers == null) {
            return target;
        }
        Map<String, Object> flat = new HashMap<>();
        delegate.toHeaders(headers, flat);
        insertInto(flat, target);
        return target;
    }

    /**
     * Inserts a flat (single-valued) map into a multi-value map: a {@code List}
     * value expands into one entry per element, anything else becomes a
     * single-element list. Null values are preserved: a Kafka header with a null
     * value (tombstone header) appears in the multi-value map as a {@code null}
     * element.
     */
    public static void insertInto(Map<String, Object> flat, MultiValueMap<String, Object> target) {
        for (Map.Entry<String, Object> entry : flat.entrySet()) {
            if (entry.getValue() instanceof List<?> list) {
                for (Object item : list) {
                    target.add(entry.getKey(), item);
                }
            } else {
                target.add(entry.getKey(), entry.getValue());
            }
        }
    }
}
