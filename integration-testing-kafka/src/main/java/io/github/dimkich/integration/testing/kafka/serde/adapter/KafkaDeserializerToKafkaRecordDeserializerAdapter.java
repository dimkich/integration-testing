package io.github.dimkich.integration.testing.kafka.serde.adapter;

import io.github.dimkich.integration.testing.kafka.config.RecordProperties;
import io.github.dimkich.integration.testing.kafka.serde.KafkaRecordSerdeFactory;
import io.github.dimkich.integration.testing.kafka.serde.StringKafkaSerdeDefaults;
import io.github.dimkich.integration.testing.kafka.serde.deserialization.KafkaRecordDeserializer;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * Adapts a native Kafka {@link Deserializer} to a record-level
 * {@link KafkaRecordDeserializer}, assembling key and header parts from the defaults.
 */
@RequiredArgsConstructor
public class KafkaDeserializerToKafkaRecordDeserializerAdapter
        implements TestSerdeAdapter<Deserializer<Object>, KafkaRecordDeserializer, RecordProperties> {

    private final KafkaRecordSerdeFactory recordSerdeFactory;

    @Override
    public KafkaRecordDeserializer adapt(Deserializer<Object> source, RecordProperties properties) {
        return recordSerdeFactory.createDeserializer(properties, source,
                StringKafkaSerdeDefaults.CORE_HEADER_DESERIALIZER);
    }
}
