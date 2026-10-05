package io.github.dimkich.integration.testing.kafka.serde.provider;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dimkich.integration.testing.kafka.config.KafkaComponentProperties;
import io.github.dimkich.integration.testing.kafka.serde.KafkaComponentRole;
import io.github.dimkich.integration.testing.kafka.serde.KafkaSerdeContext;
import io.github.dimkich.integration.testing.serde.BeanResolver;
import io.github.dimkich.integration.testing.serde.SerdeManager;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import io.github.dimkich.integration.testing.serde.TestSerdeProviderFactory;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Component-level {@code spring-json} deserializer of a record part ({@code value} or
 * {@code key}): Spring Kafka's {@link JsonDeserializer} reading type-info headers. For the
 * {@code key} role the deserializer switches to the key type mapper ({@code __KeyTypeId__}).
 */
@Getter
@ConditionalOnClass({ObjectMapper.class, JsonDeserializer.class})
public class SpringJsonKafkaValueDeserializerProvider
        implements TestSerdeProviderFactory<byte[], Object, KafkaSerdeContext, KafkaComponentRole,
        KafkaComponentProperties> {

    @Getter(AccessLevel.NONE)
    private final BeanResolver beanResolver;

    @Getter(AccessLevel.NONE)
    private final ObjectProvider<SerdeManager> serdeManager;

    private final String name;

    private final Class<byte[]> inputClass = byte[].class;

    private final Class<Object> outputClass = Object.class;

    private final Class<KafkaSerdeContext> contextClass = KafkaSerdeContext.class;

    private final Class<KafkaComponentProperties> propertiesClass = KafkaComponentProperties.class;

    @Autowired
    public SpringJsonKafkaValueDeserializerProvider(BeanResolver beanResolver,
                                                    ObjectProvider<SerdeManager> serdeManager) {
        this(beanResolver, serdeManager, "spring-json");
    }

    protected SpringJsonKafkaValueDeserializerProvider(BeanResolver beanResolver,
                                                       ObjectProvider<SerdeManager> serdeManager, String name) {
        this.beanResolver = beanResolver;
        this.serdeManager = serdeManager;
        this.name = name;
    }

    @Override
    public TestSerdeConverter<byte[], Object, KafkaSerdeContext> create(
            KafkaComponentProperties config, Class<byte[]> inputClass, Class<Object> outputClass,
            Class<KafkaSerdeContext> contextClass, @Nullable KafkaComponentRole role) {

        ObjectMapper mapper = beanResolver.resolve(config.getObjectMapperRef(), getMapperClass());
        boolean isKey = role == KafkaComponentRole.KEY;
        JavaType javaType = config.getTargetClass() != null
                ? mapper.constructType(config.getTargetClass())
                : null;
        JsonDeserializer<Object> valueDeserializer;
        if (javaType != null) {
            valueDeserializer = new JsonDeserializer<>(javaType, mapper, isKey);
        } else if (isKey) {
            valueDeserializer = new JsonDeserializer<>(Object.class, mapper, true);
        } else {
            valueDeserializer = new JsonDeserializer<>(mapper);
        }

        Map<String, Object> deserializerConfig = new HashMap<>();
        if (config.getUseTypeInfoHeaders() != null) {
            deserializerConfig.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, config.getUseTypeInfoHeaders());
        }
        if (config.getTrustedPackages() != null) {
            deserializerConfig.put(JsonDeserializer.TRUSTED_PACKAGES, config.getTrustedPackages());
        }
        if (!deserializerConfig.isEmpty()) {
            valueDeserializer.configure(deserializerConfig, isKey);
        }
        return serdeManager.getObject().adapt(valueDeserializer, config, inputClass, outputClass, contextClass,
                role);
    }

    protected Class<? extends ObjectMapper> getMapperClass() {
        return ObjectMapper.class;
    }
}
