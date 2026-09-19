package io.github.dimkich.integration.testing.kafka.serde.adapter;

import io.github.dimkich.integration.testing.kafka.serde.serialization.KafkaHeaderSerializer;
import io.github.dimkich.integration.testing.kafka.serde.serialization.SpringKafkaHeaderSerializer;
import io.github.dimkich.integration.testing.serde.SerdeProperties;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import org.springframework.kafka.support.KafkaHeaderMapper;

/**
 * Wraps a {@link KafkaHeaderMapper} (e.g. {@code DefaultKafkaHeaderMapper})
 * into a {@link KafkaHeaderSerializer} using {@link SpringKafkaHeaderSerializer}.
 */
public class KafkaHeaderMapperToKafkaHeaderSerializerAdapter
        implements TestSerdeAdapter<KafkaHeaderMapper, KafkaHeaderSerializer, SerdeProperties> {

    @Override
    public KafkaHeaderSerializer adapt(KafkaHeaderMapper source, SerdeProperties properties) {
        return new SpringKafkaHeaderSerializer(source);
    }
}
