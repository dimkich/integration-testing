package io.github.dimkich.integration.testing.kafka.serde.adapter;

import io.github.dimkich.integration.testing.kafka.config.KafkaHeaderProperties;
import io.github.dimkich.integration.testing.kafka.serde.serialization.CoreKafkaHeaderSerializer;
import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import org.apache.kafka.common.header.Headers;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * Wraps a byte-array converter into a plain-style header serializer.
 */
@Getter
@SuppressWarnings("rawtypes")
public class TestSerdeConverterToKafkaHeaderSerializerAdapter
        implements TestSerdeAdapter<TestSerdeConverter, MultiValueMap, Headers, TestSerdeContext, ComponentRole,
        KafkaHeaderProperties> {

    private final Class<TestSerdeConverter> sourceClass = TestSerdeConverter.class;

    private final Class<MultiValueMap> inputClass = MultiValueMap.class;

    private final Class<Headers> outputClass = Headers.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<KafkaHeaderProperties> propertiesClass = KafkaHeaderProperties.class;

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public TestSerdeConverter<MultiValueMap, Headers, TestSerdeContext> adapt(
            TestSerdeConverter source, KafkaHeaderProperties properties, Class<MultiValueMap> inputClass,
            Class<Headers> outputClass, Class<TestSerdeContext> contextClass,
            @Nullable ComponentRole role) {
        if (!byte[].class.equals(source.getOutputClass())) {
            return null;
        }
        return new CoreKafkaHeaderSerializer(source);
    }
}
