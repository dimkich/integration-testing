package io.github.dimkich.integration.testing.kafka.serde.adapter;

import io.github.dimkich.integration.testing.kafka.config.RecordProperties;
import io.github.dimkich.integration.testing.kafka.serde.deserialization.KafkaRecordDeserializer;
import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeDeserializer;
import lombok.RequiredArgsConstructor;

/**
 * Adapts a core {@link TestSerdeDeserializer} (with any {@link SerdeContext}
 * subtype) into a {@link KafkaRecordDeserializer}.
 * <p>
 * The wildcard {@code ? extends SerdeContext} accepts any provider that is
 * compatible with either {@code SerdeContext} or {@code KafkaSerdeContext};
 * the concrete context type is validated by
 * {@link io.github.dimkich.integration.testing.serde.TestSerdeDeserializer#assertAcceptsContext}
 * at adapt time. Providers declaring an unrelated {@code SerdeContext} subtype
 * are rejected up front instead of failing later with {@code ClassCastException}.
 * Topic and headers are packaged into {@code DefaultKafkaSerdeContext} so the
 * core deserializer receives the full context even if it ignores it.
 * <p>
 * The adapter itself is pure composition: {@link CoreToKafkaDeserializerAdapter}
 * turns the core deserializer into a native Kafka {@code Deserializer}, then
 * {@link KafkaDeserializerToKafkaRecordDeserializerAdapter} assembles the record
 * (resolving nested key/headers config and plain-style default headers).
 */
@RequiredArgsConstructor
public class TestSerdeDeserializerToKafkaRecordDeserializerAdapter
        implements TestSerdeAdapter<
        TestSerdeDeserializer<Object, ? extends SerdeContext>,
        KafkaRecordDeserializer,
        RecordProperties> {

    private final CoreToKafkaDeserializerAdapter coreToKafka;
    private final KafkaDeserializerToKafkaRecordDeserializerAdapter deserializerToRecord;

    @Override
    public KafkaRecordDeserializer adapt(TestSerdeDeserializer<Object, ? extends SerdeContext> source,
                                         RecordProperties properties) {
        return deserializerToRecord.adapt(coreToKafka.adapt(source, properties), properties);
    }
}