package io.github.dimkich.integration.testing.kafka;

import io.github.dimkich.integration.testing.IntegrationTesting;
import io.github.dimkich.integration.testing.kafka.config.KafkaConfig;
import io.github.dimkich.integration.testing.wait.completion.FutureLikeAwait;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.*;

/**
 * Enables Kafka testing support for a test class: imports message and Kafka
 * configuration, points the bootstrap servers to the test Kafka instance and makes
 * the framework wait for completed {@code KafkaProducer.send(...)} calls.
 *
 * <p>The wait-completion subsystem is activated automatically through
 * {@code kafka.properties} ({@code integration.testing.wait.completion.enabled}),
 * so no extra test properties are required.</p>
 *
 * <p>See the module documentation for the supported serde configurations.</p>
 */
@FutureLikeAwait(
        pointcut = "t.name('org.apache.kafka.clients.producer.KafkaProducer') && m.name('send')",
        await = "o.call('get')"
)
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@IntegrationTesting
@Import(KafkaConfig.class)
@TestPropertySource("classpath:kafka.properties")
public @interface EnableTestKafka {
    /**
     * Whether to enable in-flight lag tracking for this test. When {@code true},
     * {@code KafkaProducer} and {@code KafkaConsumer} are instrumented so the
     * framework can detect messages that were produced but not yet consumed.
     * This is the main and fastest mode: lag is computed from in-memory client
     * state without contacting the broker. When {@code false}, the framework
     * switches to slower Admin API checks and should be used only when client
     * instrumentation is impossible.
     *
     * @return {@code true} to enable in-flight tracking, {@code false} to disable it
     */
    boolean inflight() default true;
}
