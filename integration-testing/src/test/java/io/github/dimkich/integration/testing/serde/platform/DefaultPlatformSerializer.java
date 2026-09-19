package io.github.dimkich.integration.testing.serde.platform;

import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeSerializer;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class DefaultPlatformSerializer implements PlatformSerializer {
    private final TestSerdeSerializer<Object, ? extends SerdeContext> delegate;
    private final String marker;

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public byte[] serialize(String channel, Object data) {
        byte[] payload = ((TestSerdeSerializer) delegate)
                .serialize(data, new DefaultTestPlatformContext(channel));
        if (payload == null) {
            return null;
        }
        byte[] markerBytes = marker.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[markerBytes.length + payload.length];
        System.arraycopy(markerBytes, 0, result, 0, markerBytes.length);
        System.arraycopy(payload, 0, result, markerBytes.length, payload.length);
        return result;
    }
}
