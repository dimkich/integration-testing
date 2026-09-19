package io.github.dimkich.integration.testing.serde;

import io.github.dimkich.integration.testing.serde.adapter.SerdeAdapterResolver;
import io.github.dimkich.integration.testing.serde.resolver.SerdeResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

import java.util.function.Supplier;

/**
 * Facade of the serde subsystem for modules: resolves a serializer/deserializer from
 * {@link SerdeProperties} and adapts it to the type the module requires.
 *
 * <p>Modules should depend only on this class, not on
 * {@link SerdeResolver} or {@link SerdeAdapterResolver} directly.</p>
 */
@RequiredArgsConstructor
public class SerdeManager {
    private final SerdeResolver serdeResolver;
    private final SerdeAdapterResolver adapterResolver;

    /**
     * Resolves a serializer for the given configuration and adapts it to {@code targetType}.
     *
     * @param props serde configuration, or {@code null} to use the default
     * @param targetType the serializer type required by the module
     * @param defaultSupplier supplies the fallback serializer when {@code props} is
     *                        {@code null} or no serializer is resolved
     * @param <T> the target serializer type
     * @return the resolved and adapted serializer, or the default one
     */
    public <T> T resolveAndAdaptSerializer(@Nullable SerdeProperties props, Class<T> targetType, Supplier<T> defaultSupplier) {
        if (props == null) {
            return defaultSupplier.get();
        }
        Object raw = serdeResolver.resolveSerializer(props);
        if (raw == null) {
            return defaultSupplier.get();
        }
        return adapterResolver.adapt(raw, targetType, props);
    }

    /**
     * Resolves a deserializer for the given configuration and adapts it to {@code targetType}.
     *
     * @param props serde configuration, or {@code null} to use the default
     * @param targetType the deserializer type required by the module
     * @param defaultSupplier supplies the fallback deserializer when {@code props} is
     *                        {@code null} or no deserializer is resolved
     * @param <T> the target deserializer type
     * @return the resolved and adapted deserializer, or the default one
     */
    public <T> T resolveAndAdaptDeserializer(@Nullable SerdeProperties props, Class<T> targetType, Supplier<T> defaultSupplier) {
        if (props == null) {
            return defaultSupplier.get();
        }
        Object raw = serdeResolver.resolveDeserializer(props);
        if (raw == null) {
            return defaultSupplier.get();
        }
        return adapterResolver.adapt(raw, targetType, props);
    }
}
