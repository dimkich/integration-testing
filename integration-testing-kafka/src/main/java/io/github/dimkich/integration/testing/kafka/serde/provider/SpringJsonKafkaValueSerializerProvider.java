package io.github.dimkich.integration.testing.kafka.serde.provider;

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
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.lang.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Component-level {@code spring-json} serializer of a record part ({@code value} or {@code key}):
 * Spring Kafka's {@link JsonSerializer} with type-info headers. For the {@code key} role the
 * serializer switches to {@code __KeyTypeId__} (key type mapper).
 */
@Getter
@ConditionalOnClass({ObjectMapper.class, JsonSerializer.class})
public class SpringJsonKafkaValueSerializerProvider
        implements TestSerdeProviderFactory<Object, byte[], KafkaSerdeContext, KafkaComponentRole,
        KafkaComponentProperties> {

    @Getter(AccessLevel.NONE)
    private final BeanResolver beanResolver;

    @Getter(AccessLevel.NONE)
    private final ObjectProvider<SerdeManager> serdeManager;

    private final String name;

    private final Class<Object> inputClass = Object.class;

    private final Class<byte[]> outputClass = byte[].class;

    private final Class<KafkaSerdeContext> contextClass = KafkaSerdeContext.class;

    private final Class<KafkaComponentProperties> propertiesClass = KafkaComponentProperties.class;

    @Autowired
    public SpringJsonKafkaValueSerializerProvider(BeanResolver beanResolver,
                                                  ObjectProvider<SerdeManager> serdeManager) {
        this(beanResolver, serdeManager, "spring-json");
    }

    protected SpringJsonKafkaValueSerializerProvider(BeanResolver beanResolver,
                                                     ObjectProvider<SerdeManager> serdeManager, String name) {
        this.beanResolver = beanResolver;
        this.serdeManager = serdeManager;
        this.name = name;
    }

    @Override
    public TestSerdeConverter<Object, byte[], KafkaSerdeContext> create(
            KafkaComponentProperties config, Class<Object> inputClass, Class<byte[]> outputClass,
            Class<KafkaSerdeContext> contextClass, @Nullable KafkaComponentRole role) {

        ObjectMapper mapper = beanResolver.resolve(config.getObjectMapperRef(), getMapperClass());
        JsonSerializer<Object> valueSerializer = new JsonSerializer<>(mapper);
        boolean isKey = role == KafkaComponentRole.KEY;
        Map<String, Object> serializerConfig = new HashMap<>();
        if (config.getAddTypeInfoHeaders() != null) {
            serializerConfig.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, config.getAddTypeInfoHeaders());
        }
        if (!serializerConfig.isEmpty() || isKey) {
            valueSerializer.configure(serializerConfig, isKey);
        }
        return serdeManager.getObject().adapt(valueSerializer, config, inputClass, outputClass, contextClass,
                role);
    }

    protected Class<? extends ObjectMapper> getMapperClass() {
        return ObjectMapper.class;
    }
}
