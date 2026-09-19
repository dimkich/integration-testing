package io.github.dimkich.integration.testing.kafka.uncommitted;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;

@Slf4j
@RequiredArgsConstructor
public class UncommittedOffsetProcessor {

    private final KafkaTemplate<String, String> uncommittedKafkaTemplate;

    @KafkaListener(topics = "misc-uncommitted-in", groupId = "misc-uncommitted-group",
            containerFactory = "uncommittedListenerContainerFactory")
    public void listen(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        log.debug("Received [key={}, value={}]", record.key(), record.value());
        if ("poison".equals(record.value())) {
            throw new IllegalStateException("Poison message: " + record.key());
        }
        uncommittedKafkaTemplate.send("misc-uncommitted-out", record.key(), record.value() + " - processed");
        acknowledgment.acknowledge();
    }
}
