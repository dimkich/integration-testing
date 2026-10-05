package io.github.dimkich.integration.testing.serde;

import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;

/**
 * Facade of the serde subsystem for modules: resolves a converter from the configuration
 * ({@link #resolve}) or adapts an already available source ({@link #adapt}), decorating the
 * result in both cases.
 *
 * <p>A ready source wins over the configuration; a missing converter is an error. Modules should
 * depend only on this class, not on the managers directly.
 */
@RequiredArgsConstructor
public class SerdeManager {

    private final ConverterManager converterManager;
    private final AdapterManager adapterManager;
    private final DecoratorManager decoratorManager;

    /**
     * Resolves a converter from the configuration: validates the configuration and fails when
     * nothing can be resolved.
     *
     * @param props        serde configuration
     * @param inputClass   requested input type
     * @param outputClass  requested output type
     * @param contextClass requested context type
     * @param role         requested component role, or {@code null} for a universal one
     * @throws IllegalArgumentException if {@code props} is {@code null} or nothing can be resolved
     */
    public <I, O, C extends TestSerdeContext> TestSerdeConverter<I, O, C> resolve(
            TestSerdeProperties props, Class<I> inputClass, Class<O> outputClass,
            Class<C> contextClass, @Nullable ComponentRole role) {

        if (props == null) {
            throw new IllegalArgumentException("Serde configuration must not be null");
        }
        SerdeRoleCollector collector = new SerdeRoleCollector();
        props.reportRoles(collector);
        collector.validate();
        return converterManager.resolve(inputClass, outputClass, contextClass, props, role);
    }

    /**
     * Adapts an already available source to a converter of the requested types and applies the
     * matching decorators.
     *
     * @param source       ready source object
     * @param props        serde configuration
     * @param inputClass   requested input type
     * @param outputClass  requested output type
     * @param contextClass requested context type
     * @param role         requested component role, or {@code null} for a universal one
     * @throws IllegalArgumentException if {@code props} is {@code null} or no adapter matches
     */
    public <I, O, C extends TestSerdeContext> TestSerdeConverter<I, O, C> adapt(
            Object source, TestSerdeProperties props, Class<I> inputClass, Class<O> outputClass,
            Class<C> contextClass, @Nullable ComponentRole role) {

        if (props == null) {
            throw new IllegalArgumentException("Serde configuration must not be null");
        }
        SerdeRoleCollector collector = new SerdeRoleCollector();
        props.reportRoles(collector);
        collector.validate();
        return decoratorManager.decorate(
                adapterManager.adapt(source, props, inputClass, outputClass, contextClass, role), props,
                inputClass, outputClass, contextClass);
    }
}
