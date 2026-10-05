package io.github.dimkich.integration.testing.serde.properties;

import io.github.dimkich.integration.testing.serde.ComponentRole;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverterFactory;
import io.github.dimkich.integration.testing.serde.TestSerdeProperties;
import lombok.Getter;
import org.springframework.lang.Nullable;

/**
 * Base of built-in converter factories that handle their configuration class regardless of the
 * requested input/output types: both are declared as {@code null} ("any").
 *
 * <p>Subclasses implement
 * {@link io.github.dimkich.integration.testing.serde.TestSerdeConverterFactory#create} for a single
 * configuration type.
 *
 * @param <P> configuration type
 */
@Getter
abstract class UniversalConverterFactory<P extends TestSerdeProperties>
        implements TestSerdeConverterFactory<Object, Object, TestSerdeContext, ComponentRole, P> {

    @Nullable
    private final Class<Object> inputClass = null;

    @Nullable
    private final Class<Object> outputClass = null;

    private final Class<TestSerdeContext> contextClass = TestSerdeContext.class;
}
