package io.github.dimkich.integration.testing.kafka.config;

import lombok.Data;

import java.util.Map;

/**
 * Projection of a single connection used during the bean-definition registration
 * phase, before the Spring context is fully refreshed.
 *
 * <p>Only the fields needed to decide how connections are grouped into shared
 * Kafka clients are exposed here: the cluster address and the client properties.
 * The remaining connection configuration ({@code topics}, {@code serializer},
 * {@code deserializer}) is not used during grouping and is resolved later from
 * the fully bound {@link KafkaProperties} bean via
 * {@link KafkaProperties#getConnection(String)}.
 *
 * <p>Binding the full {@link ConnectionProperties} here is not possible: the
 * manual {@code Binder} used during {@code BeanDefinitionRegistryPostProcessor}
 * does not see {@code @ConfigurationPropertiesBinding} beans (in particular, the
 * {@code TypeConverter} for {@code java.lang.reflect.Type} fields declared in
 * {@link RecordProperties} and
 * {@link io.github.dimkich.integration.testing.serde.StandardSerdeProperties}). Binding
 * only the primitive fields avoids the conversion entirely.
 */
@Data
public class ConnectionBootstrap {
    private String bootstrapServers;
    private Map<String, Object> properties;
}