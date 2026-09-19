package io.github.dimkich.integration.testing.kafka.serde.provider;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dimkich.integration.testing.kafka.config.RecordProperties;
import io.github.dimkich.integration.testing.kafka.serde.KafkaRecordSerdeFactory;
import io.github.dimkich.integration.testing.kafka.serde.deserialization.SpringKafkaHeaderDeserializer;
import io.github.dimkich.integration.testing.kafka.serde.serialization.SpringKafkaHeaderSerializer;
import io.github.dimkich.integration.testing.serde.BeanResolver;
import io.github.dimkich.integration.testing.serde.TestSerdeProvider;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Lazy;
import org.springframework.kafka.support.DefaultKafkaHeaderMapper;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Record-level {@code spring-json} provider.
 *
 * <p>Available only at record level. Uses Spring Kafka's {@link JsonSerializer},
 * which writes type-info into {@code Headers}. This is incompatible with the
 * context-free component contract, so the provider is not registered with
 * {@link io.github.dimkich.integration.testing.serde.SerdeProperties} and
 * cannot be used as {@code value}/{@code key}/{@code headers}.type.
 *
 * <p>To combine type-info serialization with custom key or headers, use this
 * provider at record level and override {@code key}/{@code headers} nested:
 * <pre>{@code
 * serializer:
 *   type: spring-json
 *   add-type-info-headers: true
 *   key:
 *     type: io.github.dimkich.integration.testing.serde.impl.StringSerializer
 *   headers:
 *     type: org.springframework.kafka.support.DefaultKafkaHeaderMapper
 * }</pre>
 */
@Slf4j
@ConditionalOnClass({ObjectMapper.class, JsonSerializer.class})
@RequiredArgsConstructor
public class SpringJsonKafkaSerdeProvider implements TestSerdeProvider<RecordProperties> {

    private final BeanResolver beanResolver;

    @Setter(onMethod_ = {@Autowired, @Lazy})
    private KafkaRecordSerdeFactory recordSerdeFactory;

    @Setter(onMethod_ = @Autowired)
    private DefaultKafkaHeaderMapper headerMapper;

    @Override
    public String getName() {
        return "spring-json";
    }

    @Override
    public Object createSerializer(RecordProperties config) {
        ObjectMapper mapper = beanResolver.resolve(config.getObjectMapperRef(), getMapperClass());
        JsonSerializer<Object> valueSer = new JsonSerializer<>(mapper);

        Boolean addTypeInfo = config.getAddTypeInfoHeaders();
        if (addTypeInfo != null) {
            valueSer.configure(Map.of(JsonSerializer.ADD_TYPE_INFO_HEADERS, addTypeInfo), false);
        }

        return recordSerdeFactory.createSerializer(config, valueSer, new SpringKafkaHeaderSerializer(headerMapper));
    }

    @Override
    public Object createDeserializer(RecordProperties config) {
        ObjectMapper mapper = beanResolver.resolve(config.getObjectMapperRef(), getMapperClass());
        JavaType javaType = config.getTargetClass() != null
                ? mapper.constructType(config.getTargetClass())
                : null;
        JsonDeserializer<Object> valueDeser = javaType != null
                ? new JsonDeserializer<>(javaType, mapper)
                : new JsonDeserializer<>(mapper);

        Map<String, Object> deserConfig = new HashMap<>();
        if (config.getUseTypeInfoHeaders() != null) {
            deserConfig.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, config.getUseTypeInfoHeaders());
        }
        if (config.getTrustedPackages() != null) {
            deserConfig.put(JsonDeserializer.TRUSTED_PACKAGES, config.getTrustedPackages());
        }
        if (!deserConfig.isEmpty()) {
            valueDeser.configure(deserConfig, false);
        }

        return recordSerdeFactory.createDeserializer(config, valueDeser, new SpringKafkaHeaderDeserializer(headerMapper));
    }

    /**
     * Returns the mapper type used when no explicit {@code objectMapperRef} is
     * configured.
     *
     * @return the default mapper class
     */
    protected Class<? extends ObjectMapper> getMapperClass() {
        return ObjectMapper.class;
    }
}
