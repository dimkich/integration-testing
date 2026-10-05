package io.github.dimkich.integration.testing.kafka.serde;

import io.github.dimkich.integration.testing.kafka.serde.serialization.CoreKafkaHeaderSerializer;
import io.github.dimkich.integration.testing.serde.*;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.kafka.common.header.Headers;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.lang.Nullable;
import org.springframework.util.MultiValueMap;

/**
 * Fallback converter factory for the Kafka {@code headers} component: resolves the configured
 * format as a value-shaped converter ({@code Object -> byte[]}) and wraps it into the plain
 * header serializer. Native header sources ({@code spring-json}, {@code spring-xml},
 * FQCN/beanRef serializers, header mappers) are claimed earlier by providers and adapters;
 * this factory handles the formats that have no header-shaped converter of their own.
 * <p>
 * Declares no role and {@link TestSerdeProperties} as the properties class, so it sorts after
 * all other converter factories; only {@code HEADERS} requests are handled. Errors of the
 * nested resolution are not swallowed: a configuration that cannot be resolved stays an error.
 */
@Getter
@SuppressWarnings("rawtypes")
public class KafkaHeaderSerializerConverterFactory
        implements TestSerdeConverterFactory<MultiValueMap, Headers, TestSerdeContext, ComponentRole,
        TestSerdeProperties> {

    @Getter(AccessLevel.NONE)
    private final ObjectProvider<SerdeManager> serdeManager;

    private final Class<MultiValueMap> inputClass = MultiValueMap.class;

    private final Class<Headers> outputClass = Headers.class;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;

    private final Class<TestSerdeProperties> propertiesClass = TestSerdeProperties.class;

    public KafkaHeaderSerializerConverterFactory(ObjectProvider<SerdeManager> serdeManager) {
        this.serdeManager = serdeManager;
    }

    @Override
    @Nullable
    public TestSerdeConverter<MultiValueMap, Headers, TestSerdeContext> create(
            TestSerdeProperties config, Class<MultiValueMap> inputClass, Class<Headers> outputClass,
            Class<TestSerdeContext> contextClass, @Nullable ComponentRole role) {
        if (role == null || !KafkaComponentRole.HEADERS.name().equals(role.name())) {
            return null;
        }
        return new CoreKafkaHeaderSerializer(serdeManager.getObject()
                .resolve(config, Object.class, byte[].class, TestSerdeContext.class, role));
    }
}
