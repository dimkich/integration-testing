package io.github.dimkich.integration.testing.kafka.kafka;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;

@Getter
@RequiredArgsConstructor
@Accessors(fluent = true)
public class HeaderWithObject implements org.apache.kafka.common.header.Header {
    private final String key;
    private final Object object;
    private final byte[] value = new byte[0];
}
