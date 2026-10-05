package io.github.dimkich.integration.testing.serde.platform;

import java.nio.charset.StandardCharsets;

public class NativeBridgeDeserializer {
    public Object deserialize(byte[] data) {
        if (data == null) {
            return null;
        }
        String text = new String(data, StandardCharsets.UTF_8);
        return text.startsWith("N:") ? text.substring(2) : text;
    }
}
