package io.github.dimkich.integration.testing.serde.platform;

import io.github.dimkich.integration.testing.serde.TestSerdeContext;

public interface TestPlatformContext extends TestSerdeContext {
    String getChannel();
}
