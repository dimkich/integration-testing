package io.github.dimkich.integration.testing.kafka;

import io.github.dimkich.integration.testing.message.MessageDto;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import static io.github.dimkich.integration.testing.message.MessageDto.TEST_INBOUND_MESSAGE;

public class KafkaMessageMapper {
    private static final Set<String> kafkaIgnoredHeaders = Set.of("id", "timestamp", "kafka_consumer", "kafka_timestampType",
            "kafka_receivedPartitionId", "kafka_receivedTimestamp", "__TypeId__", "kafka_groupId", "kafka_offset",
            "kafka_acknowledgment", "kafka_batchConvertedHeaders");

    public static <T> MessageDto<T> toMessageDto(Message<T> message) {
        MessageDto<T> messageDto = new MessageDto<>();
        for (Map.Entry<String, Object> entry : message.getHeaders().entrySet()) {
            if (kafkaIgnoredHeaders.contains(entry.getKey())) {
                continue;
            }
            Object value = entry.getValue();
            if (value instanceof ArrayList<?> list) {
                value = list.isEmpty() ? null : list.get(list.size() - 1);
            }
            if (value == null) {
                continue;
            }
            switch (entry.getKey()) {
                case KafkaHeaders.TOPIC, KafkaHeaders.RECEIVED_TOPIC -> messageDto.getHeaders().setTopic(value);
                case KafkaHeaders.KEY, KafkaHeaders.RECEIVED_KEY -> messageDto.getHeaders().setKey(value);
                case TEST_INBOUND_MESSAGE -> messageDto.setTestInboundMessage((Boolean) value);
                default -> messageDto.getHeaders().put(entry.getKey(), value);
            }
        }
        messageDto.setPayload(message.getPayload());
        return messageDto;
    }
}
