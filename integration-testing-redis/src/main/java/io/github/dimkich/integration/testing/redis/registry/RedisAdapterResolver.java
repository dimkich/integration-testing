package io.github.dimkich.integration.testing.redis.registry;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.BiFunction;

/**
 * Resolves Redis codec and schema beans from Spring configuration references.
 * <p>
 * A reference is either a Spring bean name ({@code beanRef}) or a fully qualified class name
 * ({@code classRef}). The resolved object is then cast or adapted to the target type through a
 * fluent {@link Builder} pipeline used by {@link RedisObjectFactory}.
 */
@Component
@RequiredArgsConstructor
public class RedisAdapterResolver {
    private final ConfigurableBeanFactory beanFactory;

    /**
     * Starts the resolution pipeline by defining the target type and available adapters.
     *
     * @param targetType the expected type to cast or adapt to
     * @param adapters   list of available adapters to try if direct cast fails
     * @return a builder for further configuration
     */
    public <T, A> Builder<T, A> lookup(Class<T> targetType, List<A> adapters) {
        return new Builder<>(beanFactory, targetType, adapters != null ? adapters : List.of());
    }

    /**
     * Fluent builder for casting or adapting a resolved bean to a target type.
     *
     * @param <T> target type (e.g. {@link io.github.dimkich.integration.testing.redis.codec.RedisDataCodec})
     * @param <A> adapter type used when direct cast is not possible
     */
    public static class Builder<T, A> {
        private final ConfigurableBeanFactory beanFactory;
        private final Class<T> targetType;
        private final List<A> adapters;
        private String beanRef;
        private String classRef;
        private BiFunction<A, Object, T> logic;

        Builder(ConfigurableBeanFactory beanFactory, Class<T> targetType, List<A> adapters) {
            this.beanFactory = beanFactory;
            this.targetType = targetType;
            this.adapters = adapters;
        }

        /**
         * Sets the Spring bean name or class reference configuration sources.
         */
        public Builder<T, A> from(String beanRef, String classRef) {
            this.beanRef = beanRef;
            this.classRef = classRef;
            return this;
        }

        /**
         * Sets the adaptation function invoked for each adapter until a non-null result is returned.
         */
        public Builder<T, A> with(BiFunction<A, Object, T> logic) {
            this.logic = logic;
            return this;
        }

        /**
         * Resolves the raw object from Spring and returns it as the target type, applying adapters if necessary.
         */
        @SneakyThrows
        public T adapt() {
            Object rawObject = resolveRaw();
            if (rawObject == null) return null;

            if (targetType.isInstance(rawObject)) {
                return targetType.cast(rawObject);
            }

            if (logic != null) {
                for (A adapter : adapters) {
                    T result = logic.apply(adapter, rawObject);
                    if (result != null) return result;
                }
            }

            throw new IllegalArgumentException(String.format(
                    "Adaptation failed. Object from %s (type: %s) cannot be converted to [%s].",
                    StringUtils.hasText(beanRef) ? "bean '" + beanRef + "'" : "class '" + classRef + "'",
                    rawObject.getClass().getName(), targetType.getSimpleName()
            ));
        }

        @SneakyThrows
        private Object resolveRaw() {
            if (StringUtils.hasText(beanRef)) return beanFactory.getBean(beanRef);
            if (StringUtils.hasText(classRef)) {
                Class<?> cls = ClassUtils.forName(classRef, beanFactory.getBeanClassLoader());
                return BeanUtils.instantiateClass(cls);
            }
            return null;
        }
    }
}
