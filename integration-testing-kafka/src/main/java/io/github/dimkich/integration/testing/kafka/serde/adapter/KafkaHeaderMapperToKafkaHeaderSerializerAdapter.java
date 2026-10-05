package io.github.dimkich.integration.testing.kafka.serde.adapter;

import io.github.dimkich.integration.testing.kafka.config.KafkaHeaderProperties;
import io.github.dimkich.integration.testing.kafka.serde.serialization.SpringKafkaHeaderSerializer;
import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.support.KafkaHeaderMapper;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * Adapts a Spring {@link KafkaHeaderMapper} (e.g. {@code DefaultKafkaHeaderMapper}) to a header
 * serializer converter.
 *
 * @see SpringKafkaHeaderSerializer
 */
@Getter
@SuppressWarnings("rawtypes")
public class KafkaHeaderMapperToKafkaHeaderSerializerAdapter
        implements TestSerdeAdapter<KafkaHeaderMapper, MultiValueMap, Headers, TestSerdeContext, ComponentRole,
        KafkaHeaderProperties> {

    private final Class<KafkaHeaderMapper> sourceClass = KafkaHeaderMapper.class;

    private final Class<MultiValueMap> inputClass = MultiValueMap.class;

    private final Class<Headers> outputClass = Headers.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<KafkaHeaderProperties> propertiesClass = KafkaHeaderProperties.class;

    @Override
    public TestSerdeConverter<MultiValueMap, Headers, TestSerdeContext> adapt(
            KafkaHeaderMapper source, KafkaHeaderProperties properties, Class<MultiValueMap> inputClass,
            Class<Headers> outputClass, Class<TestSerdeContext> contextClass,
            @Nullable ComponentRole role) {
        return new SpringKafkaHeaderSerializer(source);
    }
}
