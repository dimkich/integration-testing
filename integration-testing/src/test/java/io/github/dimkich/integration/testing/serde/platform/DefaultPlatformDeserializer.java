package io.github.dimkich.integration.testing.serde.platform;

import io.github.dimkich.integration.testing.serde.SerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeDeserializer;
import lombok.RequiredArgsConstructor;

import java.nio.charset.StandardCharsets;

@RequiredArgsConstructor
public class DefaultPlatformDeserializer implements PlatformDeserializer {
    private final TestSerdeDeserializer<Object, ? extends SerdeContext> delegate;
    private final String marker;

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object deserialize(String channel, byte[] data) {
        byte[] payload = stripMarker(data);
        return ((TestSerdeDeserializer) delegate)
                .deserialize(payload, new DefaultTestPlatformContext(channel));
    }

    private byte[] stripMarker(byte[] data) {
        if (data == null) {
            return null;
        }
        byte[] markerBytes = marker.getBytes(StandardCharsets.UTF_8);
        if (data.length < markerBytes.length) {
            return data;
        }
        for (int i = 0; i < markerBytes.length; i++) {
            if (data[i] != markerBytes[i]) {
                return data;
            }
        }
        byte[] result = new byte[data.length - markerBytes.length];
        System.arraycopy(data, markerBytes.length, result, 0, result.length);
        return result;
    }
}
