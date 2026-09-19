package io.github.dimkich.integration.testing.serde.platform;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SerializedRecord {
    private final String channel;
    private final String key;
    private final String header;
    private final byte[] payload;
}
