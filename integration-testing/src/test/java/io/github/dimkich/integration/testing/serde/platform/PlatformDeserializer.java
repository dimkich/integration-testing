package io.github.dimkich.integration.testing.serde.platform;

public interface PlatformDeserializer {
    Object deserialize(String channel, byte[] data);
}
