package io.github.dimkich.integration.testing.kafka.serde.adapter;

import io.github.dimkich.integration.testing.kafka.serde.serialization.CoreKafkaHeaderSerializer;
import io.github.dimkich.integration.testing.kafka.serde.serialization.KafkaHeaderSerializer;
import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeSerializer;
import org.apache.kafka.common.serialization.Serializer;

/**
 * Wraps a native Kafka {@link Serializer} into a
 * {@link KafkaHeaderSerializer} using {@link CoreKafkaHeaderSerializer}.
 * The native serializer is adapted to a context-free
 * {@link io.github.dimkich.integration.testing.serde.TestSerdeSerializer}.
 */
public class KafkaSerializerToKafkaHeaderSerializerAdapter
        implements TestSerdeAdapter<Serializer<Object>, KafkaHeaderSerializer, SerdeProperties> {

    @Override
    public KafkaHeaderSerializer adapt(Serializer<Object> source, SerdeProperties properties) {
        return new CoreKafkaHeaderSerializer(
                new TestSerdeSerializer<>() {
                    @Override
                    public Class<SerdeContext> getContextClass() {
                        return SerdeContext.class;
                    }

                    @Override
                    public byte[] serialize(Object data, SerdeContext context) {
                        return source.serialize(null, null, data);
                    }
                });
    }
}
