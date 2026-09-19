package io.github.dimkich.integration.testing.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lifecycle manager of the Kafka sniffers: starts one sniffer per configured
 * connection on a dedicated daemon thread and stops them all when the Spring context
 * shuts down. Runs in the lowest lifecycle phase so sniffers are active before other
 * lifecycle beans start producing messages.
 */
@Slf4j
@RequiredArgsConstructor
public class KafkaSnifferManager implements SmartLifecycle {
    private final List<KafkaSnifferConsumer> sniffers;
    private ExecutorService executor;
    private volatile boolean running = false;

    @Override
    public void start() {
        for (KafkaSnifferConsumer sniffer : sniffers) {
            sniffer.init();
        }
        executor = Executors.newFixedThreadPool(sniffers.size(), r -> {
            Thread t = new Thread(r, "kafka-sniffer");
            t.setDaemon(true);
            return t;
        });
        for (KafkaSnifferConsumer sniffer : sniffers) {
            executor.submit(sniffer::run);
        }
        this.running = true;
    }

    @Override
    public void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
        this.running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MIN_VALUE;
    }
}
