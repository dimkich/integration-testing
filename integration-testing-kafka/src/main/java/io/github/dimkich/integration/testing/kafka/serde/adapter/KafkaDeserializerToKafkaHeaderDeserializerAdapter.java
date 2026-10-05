package io.github.dimkich.integration.testing.kafka.serde.adapter;

import io.github.dimkich.integration.testing.kafka.config.KafkaHeaderProperties;
import io.github.dimkich.integration.testing.kafka.serde.deserialization.CoreKafkaHeaderDeserializer;
import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.serialization.Deserializer;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * Adapts a native Kafka {@link Deserializer} used for headers: every Kafka header is
 * deserialized by the native deserializer and collected into a {@link MultiValueMap}.
 */
@Getter
@SuppressWarnings("rawtypes")
public class KafkaDeserializerToKafkaHeaderDeserializerAdapter
        implements TestSerdeAdapter<Deserializer, Headers, MultiValueMap, TestSerdeContext, ComponentRole,
        KafkaHeaderProperties> {

    private final Class<Deserializer> sourceClass = Deserializer.class;

    private final Class<Headers> inputClass = Headers.class;

    private final Class<MultiValueMap> outputClass = MultiValueMap.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<KafkaHeaderProperties> propertiesClass = KafkaHeaderProperties.class;

    @Override
    public TestSerdeConverter<Headers, MultiValueMap, TestSerdeContext> adapt(
            Deserializer source, KafkaHeaderProperties properties, Class<Headers> inputClass,
            Class<MultiValueMap> outputClass, Class<TestSerdeContext> contextClass,
            @Nullable ComponentRole role) {
        TestSerdeConverter<byte[], Object, TestSerdeContext> bytes =
                TestSerdeConverter.of(byte[].class, Object.class, TestSerdeContext.class,
                        (input, context) -> source.deserialize(null, null, input));
        return new CoreKafkaHeaderDeserializer(bytes);
    }
}
