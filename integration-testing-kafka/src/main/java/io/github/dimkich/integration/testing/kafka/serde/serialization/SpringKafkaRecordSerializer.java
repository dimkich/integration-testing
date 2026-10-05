package io.github.dimkich.integration.testing.kafka.serde.serialization;

import io.github.dimkich.integration.testing.kafka.KafkaRecord;
import io.github.dimkich.integration.testing.kafka.serde.DefaultKafkaSerdeContext;
import io.github.dimkich.integration.testing.kafka.serde.KafkaSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.util.MultiValueMap;

/**
 * Record serializer that converts a {@link KafkaRecord} into a {@link ProducerRecord} using the
 * configured key, value and header serializers.
 */
@Getter
@SuppressWarnings("rawtypes")
public class SpringKafkaRecordSerializer implements TestSerdeConverter<KafkaRecord, ProducerRecord, TestSerdeContext> {

    protected final TestSerdeConverter<Object, byte[], KafkaSerdeContext> keySerializer;
    protected final TestSerdeConverter<Object, byte[], KafkaSerdeContext> valueSerializer;
    protected final TestSerdeConverter<MultiValueMap, Headers, TestSerdeContext> headerSerializer;

    private final Class<KafkaRecord> inputClass = KafkaRecord.class;
    private final Class<ProducerRecord> outputClass = ProducerRecord.class;
    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    /**
     * Creates the serializer.
     *
     * @param keySerializer    serializer of the record key
     * @param valueSerializer  serializer of the record value
     * @param headerSerializer serializer of the record headers, may be {@code null}
     */
    public SpringKafkaRecordSerializer(TestSerdeConverter<Object, byte[], KafkaSerdeContext> keySerializer,
                                       TestSerdeConverter<Object, byte[], KafkaSerdeContext> valueSerializer,
                                       TestSerdeConverter<MultiValueMap, Headers, TestSerdeContext> headerSerializer) {
        this.keySerializer = keySerializer;
        this.valueSerializer = valueSerializer;
        this.headerSerializer = headerSerializer;
    }

    @Override
    public ProducerRecord convert(KafkaRecord record, TestSerdeContext context) {
        String topic = record.getTopic();
        Object rawKey = record.getKey();
        Object rawValue = record.getValue();

        Headers headers = new RecordHeaders();
        MultiValueMap<String, Object> msgHeaders = record.getHeaders();
        if (msgHeaders != null && !msgHeaders.isEmpty() && headerSerializer != null) {
            headers = headerSerializer.convert(msgHeaders, TestSerdeContext.EMPTY);
        }

        DefaultKafkaSerdeContext serdeContext = new DefaultKafkaSerdeContext(topic, headers);
        byte[] keyBytes = rawKey != null ? keySerializer.convert(rawKey, serdeContext) : null;
        byte[] valueBytes = rawValue != null ? valueSerializer.convert(rawValue, serdeContext) : null;

        return new ProducerRecord<>(topic, record.getPartition(), record.getTimestamp(), keyBytes, valueBytes,
                headers);
    }
}
