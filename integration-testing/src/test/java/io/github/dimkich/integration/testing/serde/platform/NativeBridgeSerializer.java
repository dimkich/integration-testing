package io.github.dimkich.integration.testing.serde.platform;

import java.nio.charset.StandardCharsets;

public class NativeBridgeSerializer {
    public byte[] serialize(Object data) {
        return data == null ? null : ("N:" + data).getBytes(StandardCharsets.UTF_8);
    }
}
