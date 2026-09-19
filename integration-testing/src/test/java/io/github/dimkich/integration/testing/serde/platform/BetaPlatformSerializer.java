package io.github.dimkich.integration.testing.serde.platform;

public interface BetaPlatformSerializer extends CommonPlatformSerializer {
    @SuppressWarnings("unused")
    byte[] serializeBeta(Object data);
}
