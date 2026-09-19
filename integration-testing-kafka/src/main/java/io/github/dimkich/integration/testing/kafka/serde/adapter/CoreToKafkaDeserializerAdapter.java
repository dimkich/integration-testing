package io.github.dimkich.integration.testing.kafka.serde.adapter;

import io.github.dimkich.integration.testing.kafka.serde.DefaultKafkaSerdeContext;
import io.github.dimkich.integration.testing.kafka.serde.KafkaSerdeContext;
import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeDeserializer;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * Wraps a core deserializer into a native Kafka {@link Deserializer}.
 * <p>
 * The wildcard {@code ? extends SerdeContext} accepts any provider that is
 * compatible with either {@code SerdeContext} or {@code KafkaSerdeContext};
 * the concrete context type is validated by
 * {@link TestSerdeDeserializer#assertAcceptsContext} at adapt time. Providers declaring an
 * unrelated {@code SerdeContext} subtype are rejected up front instead of
 * failing later with {@code ClassCastException}. Topic and headers are
 * packaged into {@link DefaultKafkaSerdeContext} so the core deserializer
 * receives the full context even if it ignores it.
 */
public class CoreToKafkaDeserializerAdapter implements TestSerdeAdapter<
        TestSerdeDeserializer<Object, ? extends SerdeContext>,
        Deserializer<Object>,
        SerdeProperties> {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Deserializer<Object> adapt(TestSerdeDeserializer<Object, ? extends SerdeContext> source,
                                      SerdeProperties properties) {
        source.assertAcceptsContext(KafkaSerdeContext.class,
                SerdeAdapterMessages.RECORD_LEVEL_PROVIDER_HINT);
        return new Deserializer<>() {
            @Override
            public Object deserialize(String topic, byte[] data) {
                return ((TestSerdeDeserializer) source).deserialize(data, new DefaultKafkaSerdeContext(topic, null));
            }

            @Override
            public Object deserialize(String topic, Headers headers, byte[] data) {
                return ((TestSerdeDeserializer) source).deserialize(data, new DefaultKafkaSerdeContext(topic, headers));
            }
        };
    }
}
