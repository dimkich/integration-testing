package io.github.dimkich.integration.testing.kafka.serde.serialization;

import io.github.dimkich.integration.testing.kafka.KafkaRecord;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.util.MultiValueMap;

/**
 * {@link KafkaRecordSerializer} backed by native Kafka {@link Serializer}s for the key
 * and value plus a {@link KafkaHeaderSerializer} for headers.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
@RequiredArgsConstructor
public class SpringKafkaRecordSerializer<K, V> implements KafkaRecordSerializer {

    protected final Serializer<K> rawKeySerializer;
    protected final Serializer<V> rawValueSerializer;
    protected final KafkaHeaderSerializer headerSerializer;

    @Override
    @SuppressWarnings("unchecked")
    public ProducerRecord<byte[], byte[]> serialize(KafkaRecord message) {
        String topic = message.getTopic();
        Object rawKey = message.getKey();
        Object rawValue = message.getValue();

        Headers headers = new RecordHeaders();
        MultiValueMap<String, Object> msgHeaders = message.getHeaders();
        if (msgHeaders != null && !msgHeaders.isEmpty() && headerSerializer != null) {
            headerSerializer.serialize(msgHeaders, headers);
        }

        byte[] keyBytes = rawKey != null ? rawKeySerializer.serialize(topic, headers, (K) rawKey) : null;
        byte[] valueBytes = rawValue != null ? rawValueSerializer.serialize(topic, headers, (V) rawValue) : null;

        return new ProducerRecord<>(topic, message.getPartition(), message.getTimestamp(), keyBytes, valueBytes, headers);
    }
}
