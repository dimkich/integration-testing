package io.github.dimkich.integration.testing.serde;

import io.github.dimkich.integration.testing.config.PropertyInheritanceExclusive;
import lombok.Data;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;

/**
 * Describes how a value should be serialized. Shared across all modules
 * (Kafka, Redis, Web).
 *
 * <p><b>Where this class applies:</b> used as the config for
 * {@code serializer.value}, {@code serializer.key}, {@code serializer.headers},
 * and as the base for record-level configs. Providers registered with
 * {@code SerdeProperties} are available at both record and component level.
 * Record-only providers (e.g. {@code spring-json}) are registered with a
 * config subclass and are not visible here.
 *
 * <p>The source of the serializer/deserializer is selected by one of three
 * mutually exclusive fields:
 * <ul>
 *   <li>{@link #beanRef} — name of an existing Spring bean. The ready-made
 *       object is taken as is; no configuration is applied.</li>
 *   <li>{@link #type} without a dot ({@code json}, {@code xml}, {@code string},
 *       {@code bytes}, ...) — name of a registered provider. The provider knows
 *       how to build a serializer/deserializer and apply
 *       {@link #targetClass} / {@link #objectMapperRef}. The set of available
 *       names depends on the module and the config level — see below.</li>
 *   <li>{@link #type} with a dot — fully qualified class name. The class is
 *       created by Spring as is, without applying {@link #targetClass} /
 *       {@link #objectMapperRef}. For configurable serializers use a provider.</li>
 * </ul>
 *
 * <p><b>Why a provider is not available everywhere.</b> A provider declares
 * which config class it works with (e.g. a record-level Kafka config).
 * Record-level providers such as {@code spring-json} need record-only settings
 * (type-info headers, trusted packages) that do not exist on
 * {@code SerdeProperties}. That is why {@code spring-json} is available at the
 * record level of the Kafka config but not at the {@code value} level: different
 * roles — assemble a whole record vs assemble a single value.
 *
 * <p>The {@code Unknown serde provider} error lists providers available
 * <i>for the current config class</i>.
 *
 * <p>A provider selected at this level produces <b>context-free</b> value
 * serialization ({@code T → byte[]} without topic, headers or other transport
 * context). For context-aware serialization (headers, type-info), use a
 * record-level provider (e.g. {@code spring-json}) or a platform interface such
 * as {@code KafkaRecordSerializer}.
 *
 * @see io.github.dimkich.integration.testing.serde.TestSerdeProvider
 * @see io.github.dimkich.integration.testing.serde.resolver.SerdeResolver
 */
@Data
public class SerdeProperties {
    @PropertyInheritanceExclusive("ref")
    private String beanRef;

    @PropertyInheritanceExclusive("ref")
    private String type;

    private Type targetClass;
    private String objectMapperRef;

    /**
     * Returns whether a reference to an existing Spring bean is configured.
     *
     * @return {@code true} if {@link #beanRef} contains text
     */
    public boolean hasBeanRef() {
        return StringUtils.hasText(beanRef);
    }

    /**
     * Returns whether a provider name or a fully qualified class name is configured.
     *
     * @return {@code true} if {@link #type} contains text
     */
    public boolean hasType() {
        return StringUtils.hasText(type);
    }

    /**
     * Validates same-level mutual exclusivity of the source-selection fields.
     * <p>
     * {@code beanRef} and {@code type} are mutually exclusive at a single
     * config level. The {@code @PropertyInheritanceExclusive("ref")} annotation
     * only governs inheritance across levels during merge; it does not guard
     * against both fields being set together at one level. Without this check
     * {@code beanRef} would win silently and {@code type} would be ignored.
     * <p>
     * {@code targetClass} / {@code objectMapperRef} are applied only on the
     * provider-name path ({@code type} without a dot). On the {@code beanRef}
     * path the ready bean is used as-is, and on the fully-qualified-class-name
     * path the class is created directly — in both cases the fields would be
     * silently ignored, so they are rejected.
     *
     * @throws IllegalArgumentException if both {@code beanRef} and {@code type} are set,
     *                                  or if {@code targetClass}/{@code objectMapperRef}
     *                                  are set on the {@code beanRef} or FQCN path
     */
    public void validate() {
        if (hasBeanRef() && hasType()) {
            throw new IllegalArgumentException(
                    "Both 'beanRef' and 'type' are set on the same serde config. "
                            + "They are mutually exclusive: 'beanRef' points to an existing bean, "
                            + "'type' points to a provider or a fully qualified class name. "
                            + "Remove one of them.");
        }
        if (hasBeanRef() && (getTargetClass() != null || StringUtils.hasText(getObjectMapperRef()))) {
            throw new IllegalArgumentException(
                    "Field 'targetClass'/'objectMapperRef' has no effect when 'beanRef' is set. "
                            + "'beanRef' returns a ready-made bean as-is; no configuration is applied. "
                            + "Use 'type' (provider name) if you need these applied.");
        }
        if (hasType() && getType().contains(".") && (getTargetClass() != null
                || StringUtils.hasText(getObjectMapperRef()))) {
            throw new IllegalArgumentException(
                    "Field 'targetClass'/'objectMapperRef' has no effect when 'type' "
                            + "is a fully qualified class name (contains a dot). "
                            + "Use a provider if you need these applied.");
        }
    }
}
