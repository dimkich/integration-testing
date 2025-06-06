package io.github.dimkich.integration.testing.kafka;

import io.github.dimkich.integration.testing.kafka.kafka.HeaderWithObject;
import io.github.dimkich.integration.testing.kafka.kafka.KafkaListenerInvoker;
import io.github.dimkich.integration.testing.message.MessageDto;
import io.github.dimkich.integration.testing.message.MessageHeadersDto;
import io.github.dimkich.integration.testing.message.TestMessageSender;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.apache.kafka.common.record.RecordBatch;
import org.apache.kafka.common.record.TimestampType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Component("kafkaTestMessageSender")
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnMissingBean(type = "kafkaTestMessageSender")
public class KafkaMockMessageSender implements TestMessageSender {
    private final Map<String, List<KafkaListenerInvoker>> topicToHandlers;

    @Override
    public boolean canSend(MessageDto<?> message) {
        if (message.getHeaders() == null || message.getHeaders().getTopic() == null) {
            return false;
        }
        return topicToHandlers.containsKey(message.getHeaders().getTopic());
    }

    @Override
    @SneakyThrows
    public void sendInboundMessage(MessageDto<?> message) {
        List<KafkaListenerInvoker> list = topicToHandlers.get(message.getHeaders().getTopic());
        if (list == null) {
            throw new RuntimeException("Unable to find callable method to send message");
        }
        for (KafkaListenerInvoker invoker : list) {
            try {
                invoker.invoke(toKafkaConsumerRecord(message));
            } catch (Exception e) {
                log.error("", e);
            }
        }
    }

    private ConsumerRecord<Object, Object> toKafkaConsumerRecord(MessageDto<?> message) {
        RecordHeaders recordHeaders = new RecordHeaders();
        for (Map.Entry<String, Object> entry : message.getHeaders().entrySet()) {
            if (MessageHeadersDto.TOPIC.equals(entry.getKey()) || MessageHeadersDto.KEY.equals(entry.getKey())) {
                continue;
            }
            recordHeaders.add(new HeaderWithObject(entry.getKey(), entry.getValue()));
        }
        return new ConsumerRecord<>((String) message.getHeaders().getTopic(), 0, 0, RecordBatch.NO_TIMESTAMP,
                TimestampType.NO_TIMESTAMP_TYPE, ConsumerRecord.NULL_SIZE, ConsumerRecord.NULL_SIZE,
                message.getHeaders().getKey(), message.getPayload(), recordHeaders, Optional.empty());
    }
}
