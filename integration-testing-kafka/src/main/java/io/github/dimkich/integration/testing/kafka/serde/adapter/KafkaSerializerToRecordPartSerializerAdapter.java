package io.github.dimkich.integration.testing.kafka.serde.adapter;

import io.github.dimkich.integration.testing.kafka.serde.KafkaSerdeContext;
import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeProperties;
import lombok.Getter;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.lang.Nullable;

/**
 * Adapts a native Kafka {@link Serializer} used as a record part (key or value): the produced
 * converter is a core serializer carrying the topic and headers of the record being written.
 */
@Getter
@SuppressWarnings({"rawtypes", "unchecked"})
public class KafkaSerializerToRecordPartSerializerAdapter
        implements TestSerdeAdapter<Serializer, Object, byte[], KafkaSerdeContext, ComponentRole,
        TestSerdeProperties> {

    private final Class<Serializer> sourceClass = Serializer.class;

    private final Class<Object> inputClass = Object.class;

    private final Class<byte[]> outputClass = byte[].class;

    private final Class<KafkaSerdeContext> contextClass = KafkaSerdeContext.class;

    private final Class<TestSerdeProperties> propertiesClass = TestSerdeProperties.class;

    @Override
    @Nullable
    public TestSerdeConverter<Object, byte[], KafkaSerdeContext> adapt(
            Serializer source, TestSerdeProperties properties, Class<Object> inputClass,
            Class<byte[]> outputClass, Class<KafkaSerdeContext> contextClass, @Nullable ComponentRole role) {
        return TestSerdeConverter.of(inputClass, outputClass, KafkaSerdeContext.class,
                (input, context) -> source.serialize(context.getTopic(), context.getHeaders(), input));
    }
}
