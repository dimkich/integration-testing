package io.github.dimkich.integration.testing.serde.platform;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DefaultTestPlatformContext implements TestPlatformContext {
    private final String channel;
}
