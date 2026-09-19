package io.github.dimkich.integration.testing.serde.platform;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultPlatformRecordDeserializer implements PlatformRecordDeserializer {
    private final PlatformDeserializer delegate;

    @Override
    public Object deserialize(String channel, String key, byte[] payload) {
        return delegate.deserialize(channel, payload);
    }
}
