package io.github.dimkich.integration.testing.serde.platform;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class DefaultPlatformRecordSerializer implements PlatformRecordSerializer {
    private final PlatformSerializer delegate;
    private final String keyPrefix;
    private final String headerName;

    @Override
    public SerializedRecord serialize(String channel, String key, Object value) {
        return new SerializedRecord(channel, keyPrefix + key, headerName, delegate.serialize(channel, value));
    }
}
