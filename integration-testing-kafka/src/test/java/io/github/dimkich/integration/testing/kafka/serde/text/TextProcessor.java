package io.github.dimkich.integration.testing.kafka.serde.text;

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
public class TextProcessor {

    private final ReceivedMessageStorage receivedMessageStorage;

    @KafkaListener(topics = "text-record-in", groupId = "text-record-group",
            containerFactory = "textListenerContainerFactory")
    public void listenRecord(@Payload(required = false) String payload,
                             @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                             @Header(name = "test-header", required = false) String testHeader,
                             @Headers Map<String, Object> headers) {
        log.debug("listenRecord: received [key={}, payload={}, header={}]", key, payload, testHeader);
        receivedMessageStorage.storeReceived("text-record-in", key, payload, headers);
    }

    @KafkaListener(topics = "text-parts-in", groupId = "text-parts-group",
            containerFactory = "textListenerContainerFactory")
    public void listenParts(@Payload(required = false) String payload,
                            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                            @Headers Map<String, Object> headers) {
        log.debug("listenParts: received [key={}, payload={}]", key, payload);
        receivedMessageStorage.storeReceived("text-parts-in", key, payload, headers);
    }

    @KafkaListener(topics = "text-native-in", groupId = "text-native-group",
            containerFactory = "textListenerContainerFactory")
    public void listenNative(@Payload(required = false) String payload,
                             @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                             @Headers Map<String, Object> headers) {
        log.debug("listenNative: received [key={}, payload={}]", key, payload);
        receivedMessageStorage.storeReceived("text-native-in", key, payload, headers);
    }

    @KafkaListener(topics = "text-string-record-in", groupId = "text-string-record-group",
            containerFactory = "textListenerContainerFactory")
    public void listenStringRecord(@Payload(required = false) String payload,
                                   @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                                   @Header(name = "test-header", required = false) String testHeader,
                                   @Headers Map<String, Object> headers) {
        log.debug("listenStringRecord: received [key={}, payload={}, header={}]", key, payload, testHeader);
        receivedMessageStorage.storeReceived("text-string-record-in", key, payload, headers);
    }

    @KafkaListener(topics = "text-default-in", groupId = "text-default-group",
            containerFactory = "textListenerContainerFactory")
    public void listenDefault(@Payload(required = false) String payload,
                              @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                              @Headers Map<String, Object> headers) {
        log.debug("listenDefault: received [key={}, payload={}]", key, payload);
        receivedMessageStorage.storeReceived("text-default-in", key, payload, headers);
    }

}
