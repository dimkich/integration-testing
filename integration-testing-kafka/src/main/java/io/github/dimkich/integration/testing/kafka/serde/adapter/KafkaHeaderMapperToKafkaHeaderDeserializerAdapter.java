package io.github.dimkich.integration.testing.kafka.serde.adapter;

import io.github.dimkich.integration.testing.kafka.serde.deserialization.KafkaHeaderDeserializer;
import io.github.dimkich.integration.testing.kafka.serde.deserialization.SpringKafkaHeaderDeserializer;
import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import org.springframework.kafka.support.KafkaHeaderMapper;

/**
 * Wraps a {@link KafkaHeaderMapper} (e.g. {@code DefaultKafkaHeaderMapper})
 * into a {@link KafkaHeaderDeserializer} using {@link SpringKafkaHeaderDeserializer}.
 */
public class KafkaHeaderMapperToKafkaHeaderDeserializerAdapter
        implements TestSerdeAdapter<KafkaHeaderMapper, KafkaHeaderDeserializer, SerdeProperties> {

    @Override
    public KafkaHeaderDeserializer adapt(KafkaHeaderMapper source, SerdeProperties properties) {
        return new SpringKafkaHeaderDeserializer(source);
    }
}
