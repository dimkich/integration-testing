package io.github.dimkich.integration.testing.kafka.serde.envelope;

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
public class EnvelopeProcessor {

    private final ReceivedMessageStorage receivedMessageStorage;

    @KafkaListener(topics = "bin-envelope-in", groupId = "bin-envelope-group",
            containerFactory = "envelopeListenerContainerFactory")
    public void listenEnvelope(@Payload(required = false) String payload,
                               @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                               @Headers Map<String, Object> headers) {
        log.debug("listenEnvelope: received [key={}, payload={}]", key, payload);
        receivedMessageStorage.storeReceived("bin-envelope-in", key, payload, headers);
    }

    @KafkaListener(topics = "bin-envelope-macro-in", groupId = "bin-envelope-macro-group",
            containerFactory = "envelopeListenerContainerFactory")
    public void listenEnvelopeMacro(@Payload(required = false) String payload,
                                    @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key,
                                    @Headers Map<String, Object> headers) {
        log.debug("listenEnvelopeMacro: received [key={}, payload={}]", key, payload);
        receivedMessageStorage.storeReceived("bin-envelope-macro-in", key, payload, headers);
    }
}
