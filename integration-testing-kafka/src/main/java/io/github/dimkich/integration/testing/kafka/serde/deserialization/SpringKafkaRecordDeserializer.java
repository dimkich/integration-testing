package io.github.dimkich.integration.testing.kafka.serde.deserialization;

import io.github.dimkich.integration.testing.kafka.KafkaRecord;
import io.github.dimkich.integration.testing.kafka.serde.DefaultKafkaSerdeContext;
import io.github.dimkich.integration.testing.kafka.serde.KafkaSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Headers;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Record deserializer that converts a {@link ConsumerRecord} into a {@link KafkaRecord} using the
 * configured key, value and header deserializers.
 */
@Getter
@SuppressWarnings("rawtypes")
public class SpringKafkaRecordDeserializer
        implements TestSerdeConverter<ConsumerRecord, KafkaRecord, TestSerdeContext> {

    protected final TestSerdeConverter<byte[], Object, KafkaSerdeContext> keyDeserializer;
    protected final TestSerdeConverter<byte[], Object, KafkaSerdeContext> valueDeserializer;
    protected final TestSerdeConverter<Headers, MultiValueMap, TestSerdeContext> headerDeserializer;

    private final Class<ConsumerRecord> inputClass = ConsumerRecord.class;
    private final Class<KafkaRecord> outputClass = KafkaRecord.class;
    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    /**
     * Creates the deserializer.
     *
     * @param keyDeserializer    deserializer of the record key
     * @param valueDeserializer  deserializer of the record value
     * @param headerDeserializer deserializer of the record headers, may be {@code null}
     */
    public SpringKafkaRecordDeserializer(TestSerdeConverter<byte[], Object, KafkaSerdeContext> keyDeserializer,
                                         TestSerdeConverter<byte[], Object, KafkaSerdeContext> valueDeserializer,
                                         TestSerdeConverter<Headers, MultiValueMap, TestSerdeContext> headerDeserializer) {
        this.keyDeserializer = keyDeserializer;
        this.valueDeserializer = valueDeserializer;
        this.headerDeserializer = headerDeserializer;
    }

    @Override
    @SuppressWarnings("unchecked")
    public KafkaRecord convert(ConsumerRecord consumerRecord, TestSerdeContext context) {
        String topic = consumerRecord.topic();
        DefaultKafkaSerdeContext serdeContext = new DefaultKafkaSerdeContext(topic, consumerRecord.headers());

        Object key = keyDeserializer.convert((byte[]) consumerRecord.key(), serdeContext);
        Object value = valueDeserializer.convert((byte[]) consumerRecord.value(), serdeContext);

        MultiValueMap<String, Object> headersMap = new LinkedMultiValueMap<>();
        if (consumerRecord.headers() != null && headerDeserializer != null) {
            MultiValueMap<String, Object> parsed = headerDeserializer.convert(consumerRecord.headers(),
                    TestSerdeContext.EMPTY);
            if (parsed != null) {
                headersMap.putAll(parsed);
            }
        }

        KafkaRecord dto = new KafkaRecord();
        dto.setTopic(topic);
        dto.setKey(key);
        dto.setValue(value);
        dto.setPartition(consumerRecord.partition());
        dto.setOffset(consumerRecord.offset());
        dto.setTimestamp(consumerRecord.timestamp());
        dto.getHeaders().putAll(headersMap);

        return dto;
    }
}
