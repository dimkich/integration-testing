package io.github.dimkich.integration.testing.kafka.serde.adapter;

import io.github.dimkich.integration.testing.kafka.config.RecordProperties;
import io.github.dimkich.integration.testing.kafka.serde.KafkaRecordSerdeFactory;
import io.github.dimkich.integration.testing.kafka.serde.StringKafkaSerdeDefaults;
import io.github.dimkich.integration.testing.kafka.serde.serialization.KafkaRecordSerializer;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Adapts a native Kafka {@link Serializer} into a {@link KafkaRecordSerializer}.
 * The source serializer is passed directly to the factory as the value serializer.
 */
@RequiredArgsConstructor
public class KafkaSerializerToKafkaRecordSerializerAdapter
        implements TestSerdeAdapter<Serializer<Object>, KafkaRecordSerializer, RecordProperties> {

    private final KafkaRecordSerdeFactory recordSerdeFactory;

    @Override
    public KafkaRecordSerializer adapt(Serializer<Object> source, RecordProperties properties) {
        return recordSerdeFactory.createSerializer(properties, source,
                StringKafkaSerdeDefaults.CORE_HEADER_SERIALIZER);
    }
}
