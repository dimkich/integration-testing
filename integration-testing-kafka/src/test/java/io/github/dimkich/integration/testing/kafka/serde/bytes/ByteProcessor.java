package io.github.dimkich.integration.testing.kafka.serde.bytes;

import io.github.dimkich.integration.testing.kafka.serde.ReceivedMessageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class ByteProcessor {

    private final ReceivedMessageStorage receivedMessageStorage;

    @KafkaListener(topics = "byte-record-in", groupId = "byte-record-group",
            containerFactory = "byteListenerContainerFactory")
    public void listenRecord(@Payload(required = false) byte[] payload,
                             @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                             @Header(name = "test-header", required = false) String testHeader,
                             @Headers Map<String, Object> headers) {
        log.debug("listenRecord: received [key={}, payloadLen={}, header={}]", key, payload != null ? payload.length : 0, testHeader);
        receivedMessageStorage.storeReceived("byte-record-in", key, payload, headers);
    }

    @KafkaListener(topics = "byte-parts-in", groupId = "byte-parts-group",
            containerFactory = "byteListenerContainerFactory")
    public void listenParts(@Payload(required = false) byte[] payload,
                            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                            @Headers Map<String, Object> headers) {
        log.debug("listenParts: received [key={}, payloadLen={}]", key, payload != null ? payload.length : 0);
        receivedMessageStorage.storeReceived("byte-parts-in", key, payload, headers);
    }

    @KafkaListener(topics = "byte-native-in", groupId = "byte-native-group",
            containerFactory = "byteListenerContainerFactory")
    public void listenNative(@Payload(required = false) byte[] payload,
                             @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                             @Headers Map<String, Object> headers) {
        log.debug("listenNative: received [key={}, payloadLen={}]", key, payload != null ? payload.length : 0);
        receivedMessageStorage.storeReceived("byte-native-in", key, payload, headers);
    }

}
