package io.github.dimkich.integration.testing.kafka.wait.completion;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.jetbrains.annotations.NotNull;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.lang.Nullable;

@RequiredArgsConstructor
public class RecordListenerInterceptor<K,V> implements RecordInterceptor<K, V> {
    private final RecordInterceptor<K, V> recordInterceptor;
    private final MessageCounter messageCounter;

    @Override
    @Nullable
    public ConsumerRecord<K, V> intercept(@NotNull ConsumerRecord<K, V> record, @NotNull Consumer<K, V> consumer) {
        if (recordInterceptor != null) {
            record = recordInterceptor.intercept(record, consumer);
        }
        return record;
    }

    @Override
    public void success(ConsumerRecord<K, V> record, @NotNull Consumer<K, V> consumer) {
        messageCounter.messageReceived(record.topic());
        if (recordInterceptor != null) {
            recordInterceptor.success(record, consumer);
        }
    }

    @Override
    public void failure(ConsumerRecord<K, V> record, @NotNull Exception exception, @NotNull Consumer<K, V> consumer) {
        messageCounter.messageReceived(record.topic());
        if (recordInterceptor != null) {
            recordInterceptor.failure(record, exception, consumer);
        }
    }

    @Override
    public void afterRecord(@NotNull ConsumerRecord<K, V> record, @NotNull Consumer<K, V> consumer) {
        if (recordInterceptor != null) {
            recordInterceptor.afterRecord(record, consumer);
        }
    }

    @Override
    public void setupThreadState(@NotNull Consumer<?, ?> consumer) {
        if (recordInterceptor != null) {
            recordInterceptor.setupThreadState(consumer);
        }
    }

    @Override
    public void clearThreadState(@NotNull Consumer<?, ?> consumer) {
        if (recordInterceptor != null) {
            recordInterceptor.clearThreadState(consumer);
        }
    }
}
