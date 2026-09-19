package io.github.dimkich.integration.testing.kafka.serde.deserialization;

import io.github.dimkich.integration.testing.kafka.KafkaRecord;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * {@link KafkaRecordDeserializer} backed by native Kafka {@link Deserializer}s for the
 * key and value plus a {@link KafkaHeaderDeserializer} for headers.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@RequiredArgsConstructor
public class SpringKafkaRecordDeserializer<K, V> implements KafkaRecordDeserializer {

    protected final Deserializer<K> rawKeyDeserializer;
    protected final Deserializer<V> rawValueDeserializer;
    protected final KafkaHeaderDeserializer headerDeserializer;

    @Override
    public KafkaRecord deserialize(ConsumerRecord<byte[], byte[]> record) {
        String topic = record.topic();

        Object key = rawKeyDeserializer.deserialize(topic, record.headers(), record.key());
        Object value = rawValueDeserializer.deserialize(topic, record.headers(), record.value());

        MultiValueMap<String, Object> headersMap = new LinkedMultiValueMap<>();
        if (record.headers() != null && headerDeserializer != null) {
            headerDeserializer.deserialize(record.headers(), headersMap);
        }

        KafkaRecord dto = new KafkaRecord();
        dto.setTopic(topic);
        dto.setKey(key);
        dto.setValue(value);
        dto.setPartition(record.partition());
        dto.setOffset(record.offset());
        dto.setTimestamp(record.timestamp());
        dto.getHeaders().putAll(headersMap);

        return dto;
    }
}
