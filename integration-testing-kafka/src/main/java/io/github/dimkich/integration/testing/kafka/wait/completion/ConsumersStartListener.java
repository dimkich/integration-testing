package io.github.dimkich.integration.testing.kafka.wait.completion;

import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.common.TopicPartition;
import org.springframework.kafka.listener.ConsumerAwareRebalanceListener;

import java.util.Collection;

@Slf4j
@RequiredArgsConstructor
public class ConsumersStartListener implements ConsumerAwareRebalanceListener {
    @Setter
    private ConsumerAwareRebalanceListener consumerAwareRebalanceListener;
    @Setter
    private ConsumerRebalanceListener consumerRebalanceListener;
    private boolean started = false;

    @SneakyThrows
    public synchronized void waitForStart() {
        if (!started) {
            log.trace("Waiting kafka consumers to start");
            wait(30_000);
        }
        log.trace("Kafka consumers started");
    }

    @Override
    public void onPartitionsRevokedBeforeCommit(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
        if (consumerAwareRebalanceListener != null) {
            consumerAwareRebalanceListener.onPartitionsRevokedBeforeCommit(consumer, partitions);
        }
        if (consumerRebalanceListener != null) {
            consumerRebalanceListener.onPartitionsRevoked(partitions);
        }
    }

    @Override
    public void onPartitionsRevokedAfterCommit(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
        if (consumerAwareRebalanceListener != null) {
            consumerAwareRebalanceListener.onPartitionsRevokedAfterCommit(consumer, partitions);
        }
    }

    @Override
    public void onPartitionsLost(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
        if (consumerAwareRebalanceListener != null) {
            consumerAwareRebalanceListener.onPartitionsLost(consumer, partitions);
        }
        if (consumerRebalanceListener != null) {
            consumerRebalanceListener.onPartitionsLost(partitions);
        }
    }

    @Override
    public synchronized void onPartitionsAssigned(Consumer<?, ?> consumer, Collection<TopicPartition> partitions) {
        if (consumerAwareRebalanceListener != null) {
            consumerAwareRebalanceListener.onPartitionsAssigned(consumer, partitions);
        }
        if (consumerRebalanceListener != null) {
            consumerRebalanceListener.onPartitionsAssigned(partitions);
        }
        started = true;
        log.trace("Partitions assigned {}", partitions);
        notifyAll();
    }

    @Override
    public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
        if (consumerAwareRebalanceListener != null) {
            consumerAwareRebalanceListener.onPartitionsRevoked(partitions);
        }
        if (consumerRebalanceListener != null) {
            consumerRebalanceListener.onPartitionsRevoked(partitions);
        }
    }

    @Override
    public synchronized void onPartitionsAssigned(Collection<TopicPartition> partitions) {
        if (consumerAwareRebalanceListener != null) {
            consumerAwareRebalanceListener.onPartitionsAssigned(partitions);
        }
        if (consumerRebalanceListener != null) {
            consumerRebalanceListener.onPartitionsAssigned(partitions);
        }
    }

    @Override
    public void onPartitionsLost(Collection<TopicPartition> partitions) {
        if (consumerAwareRebalanceListener != null) {
            consumerAwareRebalanceListener.onPartitionsLost(partitions);
        }
        if (consumerRebalanceListener != null) {
            consumerRebalanceListener.onPartitionsLost(partitions);
        }
    }
}
