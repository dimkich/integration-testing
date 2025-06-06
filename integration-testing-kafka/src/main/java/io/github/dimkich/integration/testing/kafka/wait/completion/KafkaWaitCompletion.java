package io.github.dimkich.integration.testing.kafka.wait.completion;

import io.github.dimkich.integration.testing.wait.completion.WaitCompletion;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import org.springframework.kafka.listener.BatchInterceptor;
import org.springframework.kafka.listener.RecordInterceptor;

@RequiredArgsConstructor
public class KafkaWaitCompletion implements WaitCompletion {
    private final MessageCounter messageCounter;
    @Setter
    private ConsumersStartListener consumersStartListener;

    public void addListener(String topic) {
        messageCounter.addListener(topic);
    }

    @Override
    public void start() {
        if (consumersStartListener != null) {
            consumersStartListener.waitForStart();
        }
        messageCounter.clear();
    }

    @Override
    @SneakyThrows
    public synchronized void waitCompletion() {
        messageCounter.waitAllMessageToReceive();
    }

    @Override
    public boolean isAnyTaskStarted() {
        return messageCounter.isAnyTaskStarted();
    }

    <K, V> BatchInterceptor<K, V> createBatchInterceptor(BatchInterceptor<K, V> batchInterceptor) {
        return new BatchListenerInterceptor<>(batchInterceptor, messageCounter);
    }

    <K, V> RecordInterceptor<K, V> createRecordInterceptor(RecordInterceptor<K, V> recordInterceptor) {
        return new RecordListenerInterceptor<>(recordInterceptor, messageCounter);
    }
}
