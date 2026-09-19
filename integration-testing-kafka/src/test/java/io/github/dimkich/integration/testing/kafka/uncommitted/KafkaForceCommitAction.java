package io.github.dimkich.integration.testing.kafka.uncommitted;

import io.github.dimkich.integration.testing.execution.hook.MethodAction;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.lang.reflect.Executable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class KafkaForceCommitAction implements MethodAction {

    @Override
    @SuppressWarnings("unchecked")
    public void execute(Object target, Executable method, Object[] args, Object returnValue, Throwable thrown) {
        List<ConsumerRecord<?, ?>> records = (List<ConsumerRecord<?, ?>>) args[1];
        Consumer<?, ?> consumer = (Consumer<?, ?>) args[2];
        if (records == null || records.isEmpty()) {
            return;
        }
        Map<TopicPartition, OffsetAndMetadata> offsets = new HashMap<>();
        for (ConsumerRecord<?, ?> record : records) {
            TopicPartition partition = new TopicPartition(record.topic(), record.partition());
            offsets.merge(partition, new OffsetAndMetadata(record.offset() + 1),
                    (left, right) -> left.offset() >= right.offset() ? left : right);
        }
        try {
            consumer.commitSync(offsets);
            log.debug("Force-committed offsets: {}", offsets);
        } catch (Exception e) {
            log.debug("Force commit failed: {}", e.getMessage());
        }
    }
}
