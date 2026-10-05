package io.github.dimkich.integration.testing.kafka;

import io.github.dimkich.integration.testing.kafka.registry.KafkaTopicMetadata;
import io.github.dimkich.integration.testing.kafka.registry.KafkaTopicRegistry;
import io.github.dimkich.integration.testing.message.ExceptionDto;
import io.github.dimkich.integration.testing.message.TestMessagePoller;
import io.github.dimkich.integration.testing.serde.TestSerdeContext;
import io.github.dimkich.integration.testing.serde.TestSerdeConverter;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeaders;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Background consumer that reads all non-internal topics, deserializes records with
 * the per-topic deserializer and puts the resulting {@link KafkaRecord} messages into
 * the {@link TestMessagePoller} for assertions.
 *
 * <p>Offsets of messages sent by the test itself are tracked by
 * {@link InboundMessageRegistry} and skipped when the topic is configured with
 * {@code ignoreInbound}. Deserialization failures are captured as poison records with
 * an attached {@link ExceptionDto} instead of aborting the poll loop; the original key,
 * value and header bytes are preserved so binary payloads are not corrupted.</p>
 */
@Slf4j
@RequiredArgsConstructor
@SuppressWarnings("rawtypes")
public class KafkaSnifferConsumer {

    // =====================================================================================
    // IMPORTANT: the order of the per-connection fields (the first three) is coupled to
    // KafkaConfig.PostProcessor.createBootstrapSnifferDef — explicit args are bound by
    // constructor index, and Lombok @RequiredArgsConstructor generates the constructor
    // in field declaration order.
    //
    // The first three fields are per-connection and supplied explicitly via
    // RuntimeBeanReference. The rest are singletons resolved by type via
    // AUTOWIRE_CONSTRUCTOR.
    //
    // Reordering the first three fields — update the factory as well. Reordering the
    // rest breaks nothing, but keep the semantic grouping anyway.
    // =====================================================================================

    @Getter
    private final List<String> connectionNames;
    private final KafkaConsumer<byte[], byte[]> consumer;
    private final SnifferRebalanceListener rebalanceListener;

    private final KafkaTopicRegistry topicRegistry;
    private final TestMessagePoller testMessagePoller;
    private final InboundMessageRegistry inboundMessageRegistry;

    private volatile boolean metadataNeedsRefresh = false;
    @Getter
    private volatile Set<TopicPartition> assignment = Collections.emptySet();
    @Getter
    @Setter
    private volatile Exception lastException;

    /**
     * Subscribes the sniffer to all non-internal topics using the shared rebalance
     * listener. Called once before the poll loop starts.
     */
    public void init() {
        rebalanceListener.clear();
        consumer.subscribe(Pattern.compile("^(?!__).*"), rebalanceListener);
        log.info("Sniffer [{}] started with groupId [{}]", connectionNames, consumer.groupMetadata().groupId());
    }

    /**
     * Requests a rebalance so newly created topics are picked up. Safe to call from
     * another thread; the consumer is woken up if it is currently polling.
     */
    public void signalMetadataNeedsRefresh() {
        if (!metadataNeedsRefresh) {
            metadataNeedsRefresh = true;
            consumer.wakeup();
        }
    }

    /**
     * Runs the poll loop until the thread is interrupted: polls records, processes
     * them and commits the offsets asynchronously (only when a poll returned records).
     */
    public void run() {
        while (!Thread.interrupted()) {
            if (metadataNeedsRefresh) {
                log.debug("Sniffer [{}]: Enforcing rebalance due to new topic detection", connectionNames);
                consumer.enforceRebalance();
            }
            try {
                ConsumerRecords<byte[], byte[]> records = consumer.poll(Duration.ofMillis(100));
                metadataNeedsRefresh = false;
                assignment = consumer.assignment();
                if (!records.isEmpty()) {
                    log.debug("Sniffer [{}] received {} records", connectionNames, records.count());

                    for (ConsumerRecord<byte[], byte[]> record : records) {
                        try {
                            processRecord(record);
                        } catch (Exception e) {
                            // A single bad record must not abort the batch. If it did, the loop
                            // would exit before commitSync(), leaving the committed offset behind
                            // and the unprocessed tail of the batch to be re-read after a restart
                            // or rebalance — producing duplicates in TestMessagePoller. The failure
                            // is still surfaced through lastException so KafkaWaitCompletion fails
                            // the test.
                            log.error("Sniffer [{}] failed to process record [{}@{}]",
                                    connectionNames, record.topic(), record.offset(), e);
                            lastException = e;
                        }
                    }
                    consumer.commitAsync();
                }
                updateReadPositions();
            } catch (WakeupException e) {
                log.trace("Sniffer [{}]: Poll interrupted by wakeup", connectionNames);
            } catch (Exception e) {
                log.error("Sniffer [{}] error", connectionNames, e);
                lastException = e;
            }
        }
    }

    private void updateReadPositions() {
        for (TopicPartition tp : assignment) {
            try {
                rebalanceListener.updateOffset(tp, consumer.position(tp));
            } catch (Exception e) {
                log.trace("Sniffer [{}]: cannot update read position of {}", connectionNames, tp, e);
            }
        }
    }

    private void processRecord(ConsumerRecord<byte[], byte[]> record) {
        String topic = record.topic();
        TopicPartition tp = new TopicPartition(topic, record.partition());
        rebalanceListener.updateOffset(tp, record.offset() + 1);

        for (String connectionName : connectionNames) {
            KafkaTopicMetadata metadata = topicRegistry.getMetadata(connectionName, topic);
            if (metadata.isIgnore()) {
                continue;
            }

            TestSerdeConverter<ConsumerRecord, KafkaRecord, TestSerdeContext> deserializer = metadata.getDeserializer();

            KafkaRecord kafkaRecord;
            try {
                kafkaRecord = deserializer.convert(withCopiedHeaders(record), TestSerdeContext.EMPTY);
            } catch (Exception e) {
                log.warn("Sniffer [{}]: Deserialization error on topic [{}] at offset [{}]",
                        connectionName, topic, record.offset(), e);
                kafkaRecord = createPoisonRecord(record, e);
            }
            kafkaRecord.setConnection(connectionName);

            if (metadata.isIgnoreInbound()) {
                if (inboundMessageRegistry.contains(topic, record.partition(), record.offset())) {
                    log.debug("Sniffer [{}]: Skipping inbound test message on topic [{}] at offset [{}]",
                            connectionName, topic, record.offset());
                    continue;
                }
            }

            metadata.getExcludedFields().process(kafkaRecord);

            log.debug("Sniffer [{}]: Captured message on topic [{}] at offset [{}]", connectionName, topic, record.offset());
            testMessagePoller.putMessage(kafkaRecord);
        }
    }

    /**
     * Creates a per-connection view of the record with a defensive copy of the headers.
     * <p>
     * Deserializers may mutate the passed {@link org.apache.kafka.common.header.Headers} (Spring Kafka's
     * {@code JsonDeserializer} removes type headers such as {@code __TypeId__} by
     * default). Since all connections of a sniffer share the same
     * {@link ConsumerRecord} instance, without copying, the first connection's
     * deserializer would corrupt the headers observed by the others — including the
     * {@code monitor} connection, whose purpose is to capture the untouched raw record.
     */
    private static ConsumerRecord<byte[], byte[]> withCopiedHeaders(ConsumerRecord<byte[], byte[]> record) {
        return new ConsumerRecord<>(record.topic(), record.partition(), record.offset(), record.timestamp(),
                record.timestampType(), record.serializedKeySize(), record.serializedValueSize(),
                record.key(), record.value(), new RecordHeaders(record.headers()), record.leaderEpoch());
    }

    /**
     * Creates a record that keeps the original bytes of the key, value and headers, so a
     * deserialization failure never corrupts a binary payload with a lossy UTF-8 conversion.
     * In XML expectations such values are written as Base64 with {@code type="byte[]"}.
     */
    private static KafkaRecord createPoisonRecord(ConsumerRecord<byte[], byte[]> record, Exception exception) {
        KafkaRecord dto = new KafkaRecord();
        dto.setTopic(record.topic());
        dto.setPartition(record.partition());
        dto.setOffset(record.offset());
        dto.setTimestamp(record.timestamp());
        dto.setKey(record.key());
        dto.setValue(record.value());
        for (Header header : record.headers()) {
            dto.getHeaders().add(header.key(), header.value());
        }
        dto.setException(new ExceptionDto(exception));
        return dto;
    }
}
