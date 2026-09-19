package io.github.dimkich.integration.testing.kafka.serde.adapter;

import io.github.dimkich.integration.testing.kafka.serde.deserialization.CoreKafkaHeaderDeserializer;
import io.github.dimkich.integration.testing.kafka.serde.deserialization.KafkaHeaderDeserializer;
import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeDeserializer;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * Wraps a native Kafka {@link Deserializer} into a
 * {@link KafkaHeaderDeserializer} using {@link CoreKafkaHeaderDeserializer}.
 * The native deserializer is adapted to a context-free
 * {@link io.github.dimkich.integration.testing.serde.TestSerdeDeserializer}.
 */
public class KafkaDeserializerToKafkaHeaderDeserializerAdapter
        implements TestSerdeAdapter<Deserializer<Object>, KafkaHeaderDeserializer, SerdeProperties> {

    @Override
    public KafkaHeaderDeserializer adapt(Deserializer<Object> source, SerdeProperties properties) {
        return new CoreKafkaHeaderDeserializer(
                new TestSerdeDeserializer<>() {
                    @Override
                    public Class<SerdeContext> getContextClass() {
                        return SerdeContext.class;
                    }

                    @Override
                    public Object deserialize(byte[] data, SerdeContext context) {
                        return source.deserialize(null, null, data);
                    }
                });
    }
}
