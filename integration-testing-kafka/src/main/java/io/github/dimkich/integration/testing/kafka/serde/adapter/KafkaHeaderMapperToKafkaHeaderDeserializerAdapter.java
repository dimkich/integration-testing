package io.github.dimkich.integration.testing.kafka.serde.adapter;

import io.github.dimkich.integration.testing.kafka.config.KafkaHeaderProperties;
import io.github.dimkich.integration.testing.kafka.serde.deserialization.SpringKafkaHeaderDeserializer;
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
 * deserializer converter.
 *
 * @see SpringKafkaHeaderDeserializer
 */
@Getter
@SuppressWarnings("rawtypes")
public class KafkaHeaderMapperToKafkaHeaderDeserializerAdapter
        implements TestSerdeAdapter<KafkaHeaderMapper, Headers, MultiValueMap, TestSerdeContext, ComponentRole,
        KafkaHeaderProperties> {

    private final Class<KafkaHeaderMapper> sourceClass = KafkaHeaderMapper.class;

    private final Class<Headers> inputClass = Headers.class;

    private final Class<MultiValueMap> outputClass = MultiValueMap.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<KafkaHeaderProperties> propertiesClass = KafkaHeaderProperties.class;

    @Override
    public TestSerdeConverter<Headers, MultiValueMap, TestSerdeContext> adapt(
            KafkaHeaderMapper source, KafkaHeaderProperties properties, Class<Headers> inputClass,
            Class<MultiValueMap> outputClass, Class<TestSerdeContext> contextClass,
            @Nullable ComponentRole role) {
        return new SpringKafkaHeaderDeserializer(source);
    }
}
