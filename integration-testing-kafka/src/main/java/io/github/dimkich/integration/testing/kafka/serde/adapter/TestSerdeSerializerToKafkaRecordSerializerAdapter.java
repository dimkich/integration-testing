package io.github.dimkich.integration.testing.kafka.serde.adapter;

import io.github.dimkich.integration.testing.kafka.config.RecordProperties;
import io.github.dimkich.integration.testing.kafka.serde.serialization.KafkaRecordSerializer;
import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeSerializer;
import lombok.RequiredArgsConstructor;

/**
 * Adapts a core {@link TestSerdeSerializer} (with any {@link SerdeContext}
 * subtype) into a {@link KafkaRecordSerializer}.
 * <p>
 * The wildcard {@code ? extends SerdeContext} accepts any provider that is
 * compatible with either {@code SerdeContext} or {@code KafkaSerdeContext};
 * the concrete context type is validated by
 * {@link io.github.dimkich.integration.testing.serde.TestSerdeSerializer#assertAcceptsContext}
 * at adapt time. Providers declaring an unrelated {@code SerdeContext} subtype
 * are rejected up front instead of failing later with {@code ClassCastException}.
 * Topic and headers are packaged into {@code DefaultKafkaSerdeContext} so the
 * core serializer receives the full context even if it ignores it.
 * <p>
 * The adapter itself is pure composition: {@link CoreToKafkaSerializerAdapter}
 * turns the core serializer into a native Kafka {@code Serializer}, then
 * {@link KafkaSerializerToKafkaRecordSerializerAdapter} assembles the record
 * (resolving nested key/headers config and plain-style default headers).
 */
@RequiredArgsConstructor
public class TestSerdeSerializerToKafkaRecordSerializerAdapter
        implements TestSerdeAdapter<
        TestSerdeSerializer<Object, ? extends SerdeContext>,
        KafkaRecordSerializer,
        RecordProperties> {

    private final CoreToKafkaSerializerAdapter coreToKafka;
    private final KafkaSerializerToKafkaRecordSerializerAdapter serializerToRecord;

    @Override
    public KafkaRecordSerializer adapt(TestSerdeSerializer<Object, ? extends SerdeContext> source,
                                       RecordProperties properties) {
        return serializerToRecord.adapt(coreToKafka.adapt(source, properties), properties);
    }
}