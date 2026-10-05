package io.github.dimkich.integration.testing.kafka.serde.adapter;

import io.github.dimkich.integration.testing.kafka.config.KafkaHeaderProperties;
import io.github.dimkich.integration.testing.kafka.serde.serialization.CoreKafkaHeaderSerializer;
import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * Adapts a native Kafka {@link Serializer} used for headers: every value of a
 * {@link MultiValueMap} entry is serialized by the native serializer, and the result is
 * assembled into Kafka {@link Headers}.
 */
@Getter
@SuppressWarnings({"rawtypes", "unchecked"})
public class KafkaSerializerToKafkaHeaderSerializerAdapter
        implements TestSerdeAdapter<Serializer, MultiValueMap, Headers, TestSerdeContext, ComponentRole,
        KafkaHeaderProperties> {

    private final Class<Serializer> sourceClass = Serializer.class;

    private final Class<MultiValueMap> inputClass = MultiValueMap.class;

    private final Class<Headers> outputClass = Headers.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<KafkaHeaderProperties> propertiesClass = KafkaHeaderProperties.class;

    @Override
    public TestSerdeConverter<MultiValueMap, Headers, TestSerdeContext> adapt(
            Serializer source, KafkaHeaderProperties properties, Class<MultiValueMap> inputClass,
            Class<Headers> outputClass, Class<TestSerdeContext> contextClass,
            @Nullable ComponentRole role) {
        TestSerdeConverter<Object, byte[], TestSerdeContext> bytes =
                TestSerdeConverter.of(Object.class, byte[].class, TestSerdeContext.class,
                        (input, context) -> source.serialize(null, null, input));
        return new CoreKafkaHeaderSerializer(bytes);
    }
}
