package io.github.dimkich.integration.testing.kafka.serde.adapter;

import io.github.dimkich.integration.testing.kafka.serde.KafkaSerdeContext;
import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeProperties;
import lombok.Getter;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.lang.Nullable;

/**
 * Adapts a native Kafka {@link Deserializer} used as a record part (key or value): the produced
 * converter is a core deserializer carrying the topic and headers of the record being read.
 */
@Getter
@SuppressWarnings("rawtypes")
public class KafkaDeserializerToRecordPartDeserializerAdapter
        implements TestSerdeAdapter<Deserializer, byte[], Object, KafkaSerdeContext, ComponentRole,
        TestSerdeProperties> {

    private final Class<Deserializer> sourceClass = Deserializer.class;

    private final Class<byte[]> inputClass = byte[].class;

    private final Class<Object> outputClass = Object.class;

    private final Class<KafkaSerdeContext> contextClass = KafkaSerdeContext.class;

    private final Class<TestSerdeProperties> propertiesClass = TestSerdeProperties.class;

    @Override
    @Nullable
    public TestSerdeConverter<byte[], Object, KafkaSerdeContext> adapt(
            Deserializer source, TestSerdeProperties properties, Class<byte[]> inputClass,
            Class<Object> outputClass, Class<KafkaSerdeContext> contextClass, @Nullable ComponentRole role) {
        return TestSerdeConverter.of(inputClass, outputClass, KafkaSerdeContext.class,
                (input, context) -> source.deserialize(context.getTopic(), context.getHeaders(), input));
    }
}
