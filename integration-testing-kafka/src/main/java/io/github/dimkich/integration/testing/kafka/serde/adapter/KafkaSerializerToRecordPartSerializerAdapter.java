package io.github.dimkich.integration.testing.kafka.serde.adapter;

import io.github.dimkich.integration.testing.kafka.serde.KafkaSerdeContext;
import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeProperties;
import lombok.Getter;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.lang.Nullable;

import java.nio.ByteBuffer;

/**
 * Adapts a native Kafka {@link Serializer} used as a record part (key or value): the produced
 * converter is a core serializer carrying the topic and headers of the record being written.
 * The native serializer returns {@code byte[]}, which is wrapped into a read-ready
 * {@link ByteBuffer}.
 */
@Getter
@SuppressWarnings({"rawtypes", "unchecked"})
public class KafkaSerializerToRecordPartSerializerAdapter
        implements TestSerdeAdapter<Serializer, Object, ByteBuffer, KafkaSerdeContext, ComponentRole,
        TestSerdeProperties> {

    private final Class<Serializer> sourceClass = Serializer.class;

    private final Class<Object> inputClass = Object.class;

    private final Class<ByteBuffer> outputClass = ByteBuffer.class;

    private final Class<KafkaSerdeContext> contextClass = KafkaSerdeContext.class;

    private final Class<TestSerdeProperties> propertiesClass = TestSerdeProperties.class;

    @Override
    @Nullable
    public TestSerdeConverter<Object, ByteBuffer, KafkaSerdeContext> adapt(
            Serializer source, TestSerdeProperties properties, Class<Object> inputClass,
            Class<ByteBuffer> outputClass, Class<KafkaSerdeContext> contextClass, @Nullable ComponentRole role) {
        return TestSerdeConverter.of(inputClass, outputClass, KafkaSerdeContext.class,
                (input, context) -> TestSerdeConverter.wrap(
                        source.serialize(context.getTopic(), context.getHeaders(), input)));
    }
}
