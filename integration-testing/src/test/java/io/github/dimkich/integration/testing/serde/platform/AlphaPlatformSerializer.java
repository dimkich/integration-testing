package io.github.dimkich.integration.testing.serde.platform;

public interface AlphaPlatformSerializer extends CommonPlatformSerializer {
    @SuppressWarnings("unused")
    byte[] serializeAlpha(Object data);
}
