package io.github.dimkich.integration.testing.serde;

import io.github.dimkich.integration.testing.serde.properties.ProviderProperties;
import org.springframework.lang.Nullable;

/**
 * Factory of converters for a named provider format (e.g. {@code json}, {@code spring-json}).
 *
 * <p>Providers are grouped by {@link #getName()} and matched by configuration class; the first
 * provider that creates a converter wins. {@link #tryCreate} casts the request to the declared
 * types and verifies the created converter.
 *
 * @param <I> input type
 * @param <O> output type
 * @param <C> context type
 * @param <R> role type
 * @param <P> provider configuration type
 */
public interface TestSerdeProviderFactory<I, O, C extends TestSerdeContext, R extends ComponentRole,
        P extends ProviderProperties> extends TestSerdeComponent<I, O, C, R, P> {

    /**
     * Returns the provider name used in configuration ({@code type} field).
     *
     * @return the provider name
     */
    String getName();

    /**
     * Creates a converter for the request.
     *
     * @param config       provider configuration
     * @param inputClass   requested input type
     * @param outputClass  requested output type
     * @param contextClass requested context type
     * @param role         requested role, or {@code null}
     * @return the created converter, or {@code null} when the provider does not handle the request
     */
    @Nullable
    TestSerdeConverter<I, O, C> create(P config, Class<I> inputClass, Class<O> outputClass, Class<C> contextClass,
                                       @Nullable R role);

    /**
     * Casts the request to the declared types, delegates to {@link #create} and verifies the result.
     *
     * @param config       provider configuration
     * @param inputClass   requested input type
     * @param outputClass  requested output type
     * @param contextClass requested context type
     * @param role         requested role, or {@code null}
     * @param <RI>         requested input type
     * @param <RO>         requested output type
     * @param <RC>         requested context type
     * @return the created converter, or {@code null} when the provider does not handle the request
     * @throws IllegalArgumentException if the created converter does not satisfy the request
     */
    @SuppressWarnings("unchecked")
    @Nullable
    default <RI, RO, RC extends TestSerdeContext> TestSerdeConverter<RI, RO, RC> tryCreate(
            ProviderProperties config, Class<RI> inputClass, Class<RO> outputClass, Class<RC> contextClass,
            @Nullable ComponentRole role) {
        TestSerdeConverter<I, O, C> converter = create(getPropertiesClass().cast(config), (Class<I>) inputClass,
                (Class<O>) outputClass, (Class<C>) contextClass, (R) role);
        return verify("Provider", converter, inputClass, outputClass, contextClass);
    }
}
