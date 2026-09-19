package io.github.dimkich.integration.testing.kafka.inflight.interceptor;

import io.github.dimkich.integration.testing.kafka.inflight.ledger.InFlightLedger;
import io.github.dimkich.integration.testing.kafka.util.BootstrapUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Map;

/**
 * Kafka {@link ConsumerInterceptor} that reports committed offsets to the
 * {@link InFlightLedger}, so the ledger knows how far each consumer group has caught up.
 */
@Slf4j
public class InFlightConsumerInterceptor implements ConsumerInterceptor<Object, Object> {

    private String groupId;
    private String bootstrapServers;

    @Override
    public void configure(Map<String, ?> configs) {
        groupId = (String) configs.get(ConsumerConfig.GROUP_ID_CONFIG);
        bootstrapServers = BootstrapUtil.asString(configs.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
    }

    @Override
    public ConsumerRecords<Object, Object> onConsume(ConsumerRecords<Object, Object> records) {
        return records;
    }

    @Override
    public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {
        if (offsets == null || offsets.isEmpty()) {
            return;
        }
        for (Map.Entry<TopicPartition, OffsetAndMetadata> entry : offsets.entrySet()) {
            InFlightLedger.onCommit(bootstrapServers, groupId, entry.getKey(), entry.getValue().offset());
        }
    }

    @Override
    public void close() {
    }
}
