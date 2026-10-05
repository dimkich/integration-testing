package io.github.dimkich.integration.testing.kafka.serde.provider;

import io.github.dimkich.integration.testing.kafka.config.KafkaHeaderProperties;
import io.github.dimkich.integration.testing.kafka.serde.KafkaComponentRole;
import io.github.dimkich.integration.testing.kafka.serde.deserialization.SpringKafkaHeaderDeserializer;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeProviderFactory;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.kafka.common.header.Headers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.kafka.support.DefaultKafkaHeaderMapper;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * Component-level {@code spring-json} header deserializer: delegates to the Spring Kafka
 * {@link DefaultKafkaHeaderMapper} (Spring semantics with {@code spring_json_header_types}).
 */
@Getter
@ConditionalOnClass(DefaultKafkaHeaderMapper.class)
@SuppressWarnings("rawtypes")
public class SpringJsonKafkaHeaderDeserializerProvider
        implements TestSerdeProviderFactory<Headers, MultiValueMap, TestSerdeContext, KafkaComponentRole,
        KafkaHeaderProperties> {

    @Getter(AccessLevel.NONE)
    private final DefaultKafkaHeaderMapper headerMapper;

    private final String name;

    private final Class<Headers> inputClass = Headers.class;

    private final Class<MultiValueMap> outputClass = MultiValueMap.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<KafkaHeaderProperties> propertiesClass = KafkaHeaderProperties.class;

    @Autowired
    public SpringJsonKafkaHeaderDeserializerProvider(DefaultKafkaHeaderMapper headerMapper) {
        this(headerMapper, "spring-json");
    }

    protected SpringJsonKafkaHeaderDeserializerProvider(DefaultKafkaHeaderMapper headerMapper, String name) {
        this.headerMapper = headerMapper;
        this.name = name;
    }

    @Override
    public TestSerdeConverter<Headers, MultiValueMap, TestSerdeContext> create(
            KafkaHeaderProperties config, Class<Headers> inputClass, Class<MultiValueMap> outputClass,
            Class<TestSerdeContext> contextClass, @Nullable KafkaComponentRole role) {
        return new SpringKafkaHeaderDeserializer(headerMapper);
    }
}
