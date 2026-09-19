package io.github.dimkich.integration.testing.kafka.serde.xml;

import io.github.dimkich.integration.testing.kafka.serde.ReceivedMessageStorage;
import io.github.dimkich.integration.testing.kafka.serde.json.JsonTestDto;
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
public class XmlProcessor {

    private final ReceivedMessageStorage receivedMessageStorage;

    @KafkaListener(topics = "xml-record-in", groupId = "xml-record-group",
            containerFactory = "xmlListenerContainerFactory")
    public void listenRecord(@Payload(required = false) JsonTestDto payload,
                             @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                             @Header(name = "test-header", required = false) String testHeader,
                             @Headers Map<String, Object> headers) {
        log.debug("listenRecord: received [key={}, payload={}, header={}]", key, payload, testHeader);
        receivedMessageStorage.storeReceived("xml-record-in", key, payload, headers);
    }

    @KafkaListener(topics = "xml-spring-xml-in", groupId = "xml-spring-xml-group",
            containerFactory = "xmlSpringXmlListenerContainerFactory")
    public void listenSpringXml(@Payload(required = false) Message<Object> message,
                                @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                                @Headers Map<String, Object> headers) {
        log.debug("listenSpringXml: received [key={}, payload={}]", key, message);
        receivedMessageStorage.storeReceived("xml-spring-xml-in", key, message == null ? null : message.getPayload(), headers);
    }

    @KafkaListener(topics = "xml-spring-xml-no-type-in", groupId = "xml-spring-xml-no-type-group",
            containerFactory = "xmlSpringXmlListenerContainerFactory")
    public void listenSpringXmlNoType(@Payload(required = false) Message<Object> message,
                                      @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                                      @Headers Map<String, Object> headers) {
        log.debug("listenSpringXmlNoType: received [key={}, payload={}]", key, message);
        receivedMessageStorage.storeReceived("xml-spring-xml-no-type-in", key, message == null ? null : message.getPayload(), headers);
    }

}
