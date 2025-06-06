package io.github.dimkich.integration.testing.kafka.wait.completion;

import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.listener.BatchInterceptor;

@RequiredArgsConstructor
class BatchListenerInterceptor<K, V> implements BatchInterceptor<K, V> {
    private final BatchInterceptor<K, V> batchInterceptor;
    private final MessageCounter messageCounter;

    @Override
    public ConsumerRecords<K, V> intercept(@Nonnull ConsumerRecords<K, V> records, @Nonnull Consumer<K, V> consumer) {
        if (batchInterceptor != null) {
            records = batchInterceptor.intercept(records, consumer);
        }
        return records;
    }

    @Override
    public void success(ConsumerRecords<K, V> records, Consumer<K, V> consumer) {
        records.forEach(record -> messageCounter.messageReceived(record.topic()));
        if (batchInterceptor != null) {
            batchInterceptor.success(records, consumer);
        }
    }

    @Override
    public void failure(ConsumerRecords<K, V> records, Exception exception, Consumer<K, V> consumer) {
        records.forEach(record -> messageCounter.messageReceived(record.topic()));
        if (batchInterceptor != null) {
            batchInterceptor.failure(records, exception, consumer);
        }
    }

    @Override
    public void setupThreadState(Consumer<?, ?> consumer) {
        if (batchInterceptor != null) {
            batchInterceptor.setupThreadState(consumer);
        }
    }

    @Override
    public void clearThreadState(Consumer<?, ?> consumer) {
        if (batchInterceptor != null) {
            batchInterceptor.clearThreadState(consumer);
        }
    }
}
