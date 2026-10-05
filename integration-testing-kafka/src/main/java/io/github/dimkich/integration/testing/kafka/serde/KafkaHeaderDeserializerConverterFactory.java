package io.github.dimkich.integration.testing.kafka.serde;

import io.github.dimkich.integration.testing.kafka.serde.deserialization.CoreKafkaHeaderDeserializer;
import io.github.dimkich.integration.testing.serde.*;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.kafka.common.header.Headers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * Fallback converter factory for the Kafka {@code headers} component: resolves the configured
 * format as a value-shaped converter ({@code byte[] -> Object}) and wraps it into the plain
 * header deserializer. Native header sources ({@code spring-json}, {@code spring-xml},
 * FQCN/beanRef deserializers, header mappers) are claimed earlier by providers and adapters;
 * this factory handles the formats that have no header-shaped converter of their own.
 * <p>
 * Declares no role and {@link TestSerdeProperties} as the properties class, so it sorts after
 * all other converter factories; only {@code HEADERS} requests are handled. Errors of the
 * nested resolution are not swallowed: a configuration that cannot be resolved stays an error.
 */
@Getter
@SuppressWarnings("rawtypes")
public class KafkaHeaderDeserializerConverterFactory
        implements TestSerdeConverterFactory<Headers, MultiValueMap, TestSerdeContext, ComponentRole,
        TestSerdeProperties> {

    @Getter(AccessLevel.NONE)
    private final ObjectProvider<SerdeManager> serdeManager;

    private final Class<Headers> inputClass = Headers.class;

    private final Class<MultiValueMap> outputClass = MultiValueMap.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<TestSerdeProperties> propertiesClass = TestSerdeProperties.class;

    public KafkaHeaderDeserializerConverterFactory(ObjectProvider<SerdeManager> serdeManager) {
        this.serdeManager = serdeManager;
    }

    @Override
    @Nullable
    public TestSerdeConverter<Headers, MultiValueMap, TestSerdeContext> create(
            TestSerdeProperties config, Class<Headers> inputClass, Class<MultiValueMap> outputClass,
            Class<TestSerdeContext> contextClass, @Nullable ComponentRole role) {
        if (role == null || !KafkaComponentRole.HEADERS.name().equals(role.name())) {
            return null;
        }
        return new CoreKafkaHeaderDeserializer(serdeManager.getObject()
                .resolve(config, byte[].class, Object.class, TestSerdeContext.class, role));
    }
}
