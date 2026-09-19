package io.github.dimkich.integration.testing.serde.platform;

public interface PlatformSerializer {
    byte[] serialize(String channel, Object data);
}
