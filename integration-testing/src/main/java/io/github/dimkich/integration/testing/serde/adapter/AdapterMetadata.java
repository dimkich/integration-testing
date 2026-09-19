package io.github.dimkich.integration.testing.serde.adapter;

import io.github.dimkich.integration.testing.serde.TestSerdeAdapter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Metadata of a registered {@link TestSerdeAdapter}: the source, target and config
 * classes it was registered for, plus the adapter instance itself. Used by the
 * adapter resolver to select adapters.
 */
@Getter
@RequiredArgsConstructor
public class AdapterMetadata {
    private final Class<?> sourceClass;
    private final Class<?> targetClass;
    private final Class<?> propertiesClass;
    private final TestSerdeAdapter<?, ?, ?> instance;
}
