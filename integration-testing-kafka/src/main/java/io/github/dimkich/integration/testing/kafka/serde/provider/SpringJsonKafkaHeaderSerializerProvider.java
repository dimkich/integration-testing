package io.github.dimkich.integration.testing.kafka.serde.provider;

import io.github.dimkich.integration.testing.kafka.config.KafkaHeaderProperties;
import io.github.dimkich.integration.testing.kafka.serde.KafkaComponentRole;
import io.github.dimkich.integration.testing.kafka.serde.serialization.SpringKafkaHeaderSerializer;
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
 * Component-level {@code spring-json} header serializer: delegates to the Spring Kafka
 * {@link DefaultKafkaHeaderMapper} (plain Spring semantics with {@code spring_json_header_types}).
 */
@Getter
@ConditionalOnClass(DefaultKafkaHeaderMapper.class)
@SuppressWarnings("rawtypes")
public class SpringJsonKafkaHeaderSerializerProvider
        implements TestSerdeProviderFactory<MultiValueMap, Headers, TestSerdeContext, KafkaComponentRole,
        KafkaHeaderProperties> {

    @Getter(AccessLevel.NONE)
    private final DefaultKafkaHeaderMapper headerMapper;

    private final String name;

    private final Class<MultiValueMap> inputClass = MultiValueMap.class;

    private final Class<Headers> outputClass = Headers.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<KafkaHeaderProperties> propertiesClass = KafkaHeaderProperties.class;

    @Autowired
    public SpringJsonKafkaHeaderSerializerProvider(DefaultKafkaHeaderMapper headerMapper) {
        this(headerMapper, "spring-json");
    }

    protected SpringJsonKafkaHeaderSerializerProvider(DefaultKafkaHeaderMapper headerMapper, String name) {
        this.headerMapper = headerMapper;
        this.name = name;
    }

    @Override
    public TestSerdeConverter<MultiValueMap, Headers, TestSerdeContext> create(
            KafkaHeaderProperties config, Class<MultiValueMap> inputClass, Class<Headers> outputClass,
            Class<TestSerdeContext> contextClass, @Nullable KafkaComponentRole role) {
        return new SpringKafkaHeaderSerializer(headerMapper);
    }
}
