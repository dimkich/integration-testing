package io.github.dimkich.integration.testing.kafka.serde.misc;

import io.github.dimkich.integration.testing.kafka.OrderDto;
import io.github.dimkich.integration.testing.kafka.OrderEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.support.MessageBuilder;

@Slf4j
@RequiredArgsConstructor
public class MiscProcessor {

    private final KafkaTemplate<String, Object> jsonKafkaTemplate;
    private final KafkaTemplate<String, String> textKafkaTemplate;
    private final KafkaTemplate<String, String> txOffsetsKafkaTemplate;

    @KafkaListener(topicPattern = "misc-pattern-service-.*", groupId = "misc-pattern-group",
            containerFactory = "jsonListenerContainerFactory")
    public void listenPattern(@Payload OrderDto payload,
                              @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        log.debug("listenPattern: received [key={}, payload={}]", key, payload);
        OrderEventDto event = new OrderEventDto(payload.getId(), "PATTERN_MATCHED");
        sendResponse("misc-pattern-processed", key, event);
    }

    @KafkaListener(topics = "misc-slow", groupId = "misc-slow-group",
            containerFactory = "textListenerContainerFactory")
    public void listenSlow(@Payload String text,
                           @Header(KafkaHeaders.RECEIVED_KEY) String key) throws InterruptedException {
        log.debug("listenSlow: received [key={}, text={}]", key, text);
        Thread.sleep(1000);
        var builder = MessageBuilder.withPayload(text)
                .setHeader(KafkaHeaders.TOPIC, "misc-slow-result")
                .setHeader(KafkaHeaders.KEY, key);
        textKafkaTemplate.send(builder.build());
    }

    @KafkaListener(topics = "misc-cold-start", groupId = "misc-cold-start-group",
            containerFactory = "jsonListenerContainerFactory")
    public void listenColdStart(@Payload OrderDto order,
                                @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        log.debug("listenColdStart: received [key={}, orderId={}]", key, order.getId());
        jsonKafkaTemplate.send(MessageBuilder
                .withPayload(order)
                .setHeader(KafkaHeaders.TOPIC, "misc-cold-start-result")
                .setHeader(KafkaHeaders.KEY, key)
                .build());
    }

    @KafkaListener(
            topicPartitions = @TopicPartition(topic = "misc-assign", partitions = "0"),
            groupId = "misc-assign-group",
            containerFactory = "jsonListenerContainerFactory"
    )
    public void listenAssign(@Payload OrderDto payload,
                             @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        log.debug("listenAssign: received [key={}, payload={}]", key, payload);
        OrderEventDto event = new OrderEventDto(payload.getId(), "ASSIGN_PROCESSED");
        sendResponse("misc-assign-result", key, event);
    }

    @KafkaListener(
            topics = "misc-tx-offsets-in",
            groupId = "misc-tx-offsets-group",
            containerFactory = "txOffsetsListenerContainerFactory"
    )
    public void listenTxOffsets(@Payload String text,
                                @Header(KafkaHeaders.RECEIVED_KEY) String key) {
        log.debug("listenTxOffsets: received [key={}, text={}]", key, text);
        txOffsetsKafkaTemplate.send("misc-tx-offsets-out", key, text + " - processed");
    }

    private void sendResponse(String topic, String key, Object payload) {
        var builder = MessageBuilder.withPayload(payload != null ? payload : "")
                .setHeader(KafkaHeaders.TOPIC, topic);
        if (key != null) {
            builder.setHeader(KafkaHeaders.KEY, key);
        }
        jsonKafkaTemplate.send(builder.build());
    }
}
