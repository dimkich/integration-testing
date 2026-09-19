package io.github.dimkich.integration.testing.serde.platform;

import java.nio.charset.StandardCharsets;

public class DirectPlatformDeserializer implements PlatformDeserializer {
    private final String marker;

    public DirectPlatformDeserializer() {
        this("D:");
    }

    public DirectPlatformDeserializer(String marker) {
        this.marker = marker;
    }

    @Override
    public Object deserialize(String channel, byte[] data) {
        if (data == null) {
            return null;
        }
        String text = new String(data, StandardCharsets.UTF_8);
        return text.startsWith(marker) ? text.substring(marker.length()) : text;
    }
}
