package io.github.dimkich.integration.testing.kafka.serde.json;

import io.github.dimkich.integration.testing.kafka.serde.ReceivedMessageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class JsonProcessor {

    private final ReceivedMessageStorage receivedMessageStorage;

    @KafkaListener(topics = "json-record-in", groupId = "json-record-group",
            containerFactory = "jsonListenerContainerFactory")
    public void listenRecord(@Payload(required = false) JsonTestDto payload,
                             @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                             @Header(name = "test-header", required = false) String testHeader,
                             @Headers Map<String, Object> headers) {
        log.debug("listenRecord: received [key={}, payload={}, header={}]", key, payload, testHeader);
        receivedMessageStorage.storeReceived("json-record-in", key, payload, headers);
    }

    @KafkaListener(topics = "json-parts-in", groupId = "json-parts-group",
            containerFactory = "jsonListenerContainerFactory")
    public void listenParts(@Payload(required = false) JsonTestDto payload,
                            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                            @Headers Map<String, Object> headers) {
        log.debug("listenParts: received [key={}, payload={}]", key, payload);
        receivedMessageStorage.storeReceived("json-parts-in", key, payload, headers);
    }

    @KafkaListener(topics = "json-spring-json-in", groupId = "json-spring-json-group",
            containerFactory = "jsonSpringJsonListenerContainerFactory")
    public void listenSpringJson(@Payload(required = false) Message<Object> message,
                                 @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                                 @Headers Map<String, Object> headers) {
        log.debug("listenSpringJson: received [key={}, payload={}]", key, message);
        receivedMessageStorage.storeReceived("json-spring-json-in", key, message == null ? null : message.getPayload(), headers);
    }

    @KafkaListener(topics = "json-spring-json-no-type-in", groupId = "json-spring-json-no-type-group",
            containerFactory = "jsonSpringJsonListenerContainerFactory")
    public void listenSpringJsonNoType(@Payload(required = false) Message<Object> message,
                                       @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                                       @Headers Map<String, Object> headers) {
        log.debug("listenSpringJsonNoType: received [key={}, payload={}]", key, message);
        receivedMessageStorage.storeReceived("json-spring-json-no-type-in", key, message == null ? null : message.getPayload(), headers);
    }

}
