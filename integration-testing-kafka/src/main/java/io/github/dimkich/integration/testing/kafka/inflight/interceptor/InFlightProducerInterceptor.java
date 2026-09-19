package io.github.dimkich.integration.testing.kafka.inflight.interceptor;

import io.github.dimkich.integration.testing.kafka.inflight.ledger.InFlightLedger;
import io.github.dimkich.integration.testing.kafka.util.BootstrapUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerInterceptor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.Map;

/**
 * Kafka {@link ProducerInterceptor} that feeds producer sends, acknowledgements and
 * transactional offsets into the {@link InFlightLedger}.
 */
@Slf4j
public class InFlightProducerInterceptor implements ProducerInterceptor<Object, Object> {

    private String bootstrapServers;
    private String transactionalId;
    private boolean transactional;

    /**
     * Captures the producer's {@code bootstrap.servers} and {@code transactional.id}.
     *
     * <p>{@code transactional.id} doubles as the key under which this producer's
     * pending transactional offsets are buffered. It already lives in the producer's
     * own config, so no separate per-producer id needs to be injected into (and
     * thereby leak into) the user configuration.
     */
    @Override
    public void configure(Map<String, ?> configs) {
        bootstrapServers = BootstrapUtil.asString(configs.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        transactionalId = (String) configs.get(ProducerConfig.TRANSACTIONAL_ID_CONFIG);
        transactional = configs.containsKey(ProducerConfig.TRANSACTIONAL_ID_CONFIG);
    }

    /**
     * Registers the send for lag tracking. Transactional sends are skipped: their
     * offsets are buffered on acknowledgement and flushed only at
     * {@code commitTransaction} (see {@link #onAcknowledgement}).
     */
    @Override
    public ProducerRecord<Object, Object> onSend(ProducerRecord<Object, Object> record) {
        if (!transactional) {
            InFlightLedger.onSend(bootstrapServers, record.topic(), record.partition());
        }
        return record;
    }

    /**
     * Resolves one acknowledgement. On success, it records the broker end offset
     * (non-transactional) or buffers the pending offset (transactional). On failure,
     * it still resolves the in-flight send so the topic's pending count does not
     * leak. A {@code null} metadata (failure before topic/partition known) is
     * dropped as a coarse per-connection pending cleanup, since the failed record's
     * topic is not available to resolve.
     */
    @Override
    public void onAcknowledgement(RecordMetadata metadata, Exception exception) {
        if (metadata == null) {
            if (exception != null && InFlightLedger.clearPendingTopics(bootstrapServers)) {
                log.warn("Send failed before metadata available for [{}]: {}. Cleared pending in-flight state.",
                        bootstrapServers, exception.toString());
            }
            return;
        }
        if (exception != null) {
            log.debug("onAcknowledgement error for [{}]: {}", bootstrapServers, exception.toString());
            if (!transactional) {
                InFlightLedger.completeSend(bootstrapServers, metadata.topic());
            }
            return;
        }
        TopicPartition tp = new TopicPartition(metadata.topic(), metadata.partition());
        if (transactional) {
            InFlightLedger.recordPendingEndOffset(bootstrapServers, transactionalId, tp, metadata.offset());
        } else {
            InFlightLedger.recordEndOffset(bootstrapServers, tp, metadata.offset());
            InFlightLedger.completeSend(bootstrapServers, metadata.topic());
        }
    }

    /**
     * Discards buffered transactional offsets when the producer is closed. A
     * producer may be closed without committing/aborting (e.g. abandoned
     * transaction or container teardown); without this, its pending offsets would
     * leak in this cluster's transaction tracker.
     */
    @Override
    public void close() {
        if (transactional) {
            InFlightLedger.discardPendingEndOffsets(bootstrapServers, transactionalId);
        }
    }
}
