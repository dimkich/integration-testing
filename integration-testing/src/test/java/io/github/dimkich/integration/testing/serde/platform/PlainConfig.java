package io.github.dimkich.integration.testing.serde.platform;

import io.github.dimkich.integration.testing.serde.TestSerdeProperties;
import lombok.Data;

@Data
public class PlainConfig implements TestSerdeProperties {
    private final String format;
}
