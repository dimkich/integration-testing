package io.github.dimkich.integration.testing.serde.platform;

import java.nio.charset.StandardCharsets;

public class DirectPlatformSerializer implements PlatformSerializer {
    private final String marker;

    public DirectPlatformSerializer() {
        this("D:");
    }

    public DirectPlatformSerializer(String marker) {
        this.marker = marker;
    }

    @Override
    public byte[] serialize(String channel, Object data) {
        return data == null ? null : (marker + data).getBytes(StandardCharsets.UTF_8);
    }
}
