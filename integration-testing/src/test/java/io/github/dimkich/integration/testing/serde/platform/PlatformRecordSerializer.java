package io.github.dimkich.integration.testing.serde.platform;

public interface PlatformRecordSerializer {
    SerializedRecord serialize(String channel, String key, Object value);
}
