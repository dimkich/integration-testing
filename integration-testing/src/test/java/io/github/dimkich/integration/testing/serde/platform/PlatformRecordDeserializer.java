package io.github.dimkich.integration.testing.serde.platform;

public interface PlatformRecordDeserializer {
    Object deserialize(String channel, String key, byte[] payload);
}
