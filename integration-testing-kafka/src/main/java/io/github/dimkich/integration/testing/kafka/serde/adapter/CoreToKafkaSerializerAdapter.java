package io.github.dimkich.integration.testing.kafka.serde.adapter;

import io.github.dimkich.integration.testing.kafka.serde.DefaultKafkaSerdeContext;
import io.github.dimkich.integration.testing.kafka.serde.KafkaSerdeContext;
import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeSerializer;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Wraps a core serializer into a native Kafka {@link Serializer}.
 * <p>
 * The wildcard {@code ? extends SerdeContext} accepts any provider that is
 * compatible with either {@code SerdeContext} or {@code KafkaSerdeContext};
 * the concrete context type is validated by
 * {@link TestSerdeSerializer#assertAcceptsContext} at adapt time. Providers declaring an
 * unrelated {@code SerdeContext} subtype are rejected up front instead of
 * failing later with {@code ClassCastException}. Topic and headers are
 * packaged into {@link DefaultKafkaSerdeContext} so the core serializer
 * receives the full context even if it ignores it.
 */
public class CoreToKafkaSerializerAdapter implements TestSerdeAdapter<
        TestSerdeSerializer<Object, ? extends SerdeContext>,
        Serializer<Object>,
        SerdeProperties> {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Serializer<Object> adapt(TestSerdeSerializer<Object, ? extends SerdeContext> source,
                                    SerdeProperties properties) {
        source.assertAcceptsContext(KafkaSerdeContext.class,
                SerdeAdapterMessages.RECORD_LEVEL_PROVIDER_HINT);
        return new Serializer<>() {
            @Override
            public byte[] serialize(String topic, Object data) {
                return ((TestSerdeSerializer) source).serialize(data, new DefaultKafkaSerdeContext(topic, null));
            }

            @Override
            public byte[] serialize(String topic, Headers headers, Object data) {
                return ((TestSerdeSerializer) source).serialize(data, new DefaultKafkaSerdeContext(topic, headers));
            }
        };
    }
}
